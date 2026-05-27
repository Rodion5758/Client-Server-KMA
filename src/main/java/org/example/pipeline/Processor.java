package org.example.pipeline;

import org.example.domain.CommandPayload;
import org.example.domain.CommandType;
import org.example.domain.ResponsePayload;
import org.example.domain.Warehouse;
import org.example.protocol.Message;

import java.util.concurrent.BlockingQueue;

public class Processor {
    private final BlockingQueue<Message<CommandPayload>> inQ;
    private final BlockingQueue<Message<ResponsePayload>> outQ;
    private final Warehouse warehouse;

    private Thread thread;

    public Processor(BlockingQueue<Message<CommandPayload>> inQ,
                     BlockingQueue<Message<ResponsePayload>> outQ,
                     Warehouse warehouse) {
        this.inQ = inQ;
        this.outQ = outQ;
        this.warehouse = warehouse;
    }

    public void start() {
        thread = new Thread(this::run, "Processor");
        thread.start();
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    public void process(Message<CommandPayload> message) throws InterruptedException {
        ResponsePayload result = handle(message.getCType(), message.getPayload());
        outQ.put(new Message<>(message.getCType(), message.getBUserId(), result));
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                process(inQ.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("Processor error: " + e.getMessage());
            }
        }
    }

    private ResponsePayload handle(int cType, CommandPayload c) {
        return switch (cType) {
            case CommandType.GET_QUANTITY -> warehouse.getQuantity(c.getProductName());
            case CommandType.SUBTRACT -> warehouse.subtract(c.getProductName(), c.getQuantity());
            case CommandType.ADD -> warehouse.add(c.getProductName(), c.getQuantity());
            case CommandType.ADD_GROUP -> warehouse.addGroup(c.getGroupName());
            case CommandType.ADD_PRODUCT_TO_GROUP -> warehouse.addProductToGroup(c.getGroupName(), c.getProductName());
            case CommandType.SET_PRICE -> warehouse.setPrice(c.getProductName(), c.getPrice());
            default -> ResponsePayload.fail("Unknown command type: " + cType);
        };
    }
}
