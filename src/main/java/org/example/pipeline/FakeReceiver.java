package org.example.pipeline;

import org.example.PacketEncoder;
import org.example.domain.CommandPayload;
import org.example.domain.CommandType;
import org.example.protocol.Message;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class FakeReceiver implements Receiver {
    private static final String[] PRODUCTS = {"grechka", "milk", "bread", "sugar", "salt"};
    private static final String[] GROUPS = {"food", "drinks", "spices"};

    private final BlockingQueue<byte[]> sink;
    private final PacketEncoder encoder;
    private final Random random = new Random();
    private final long sleepMillis;

    private Thread thread;

    public FakeReceiver(BlockingQueue<byte[]> sink, PacketEncoder encoder, long sleepMillis) {
        this.sink = sink;
        this.encoder = encoder;
        this.sleepMillis = sleepMillis;
    }

    @Override
    public void receiveMessage() {
        try {
            byte[] bytes = generate();
            sink.put(bytes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("FakeReceiver error: " + e.getMessage());
        }
    }

    @Override
    public void start() {
        thread = new Thread(this::run, "FakeReceiver");
        thread.start();
    }

    @Override
    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            receiveMessage();
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private byte[] generate() throws Exception {
        int cType = 1 + random.nextInt(6);
        CommandPayload payload = switch (cType) {
            case CommandType.GET_QUANTITY -> new CommandPayload(pickProduct(), null, null, null);
            case CommandType.SUBTRACT -> new CommandPayload(pickProduct(), null, 1 + random.nextInt(5), null);
            case CommandType.ADD -> new CommandPayload(pickProduct(), null, 1 + random.nextInt(10), null);
            case CommandType.ADD_GROUP -> new CommandPayload(null, pickGroup(), null, null);
            case CommandType.ADD_PRODUCT_TO_GROUP -> new CommandPayload(pickProduct(), pickGroup(), null, null);
            case CommandType.SET_PRICE -> new CommandPayload(pickProduct(), null, null, 1.0 + random.nextInt(100));
            default -> throw new IllegalStateException("unreachable");
        };
        Message<CommandPayload> msg = new Message<>(cType, random.nextInt(100), payload);
        return encoder.encode((byte) (1 + random.nextInt(5)), msg);
    }

    private String pickProduct() { return PRODUCTS[random.nextInt(PRODUCTS.length)]; }
    private String pickGroup() { return GROUPS[random.nextInt(GROUPS.length)]; }
}
