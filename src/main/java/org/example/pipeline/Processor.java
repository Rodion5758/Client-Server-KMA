package org.example.pipeline;

import org.example.domain.CommandHandler;
import org.example.domain.CommandPayload;
import org.example.domain.ResponsePayload;
import org.example.domain.Warehouse;
import org.example.protocol.Message;

import java.util.concurrent.BlockingQueue;

public class Processor {
    private final BlockingQueue<Message<CommandPayload>> inQ;
    private final BlockingQueue<Message<ResponsePayload>> outQ;
    private final CommandHandler handler;

    private Thread thread;

    public Processor(BlockingQueue<Message<CommandPayload>> inQ,
                     BlockingQueue<Message<ResponsePayload>> outQ,
                     Warehouse warehouse) {
        this.inQ = inQ;
        this.outQ = outQ;
        this.handler = new CommandHandler(warehouse);
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
        outQ.put(handler.handle(message));
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
}
