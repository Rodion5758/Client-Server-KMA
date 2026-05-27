package org.example.pipeline;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class FakeSender implements Sender {
    private final BlockingQueue<byte[]> source;
    private final AtomicLong sentCount = new AtomicLong();

    private Thread thread;

    public FakeSender(BlockingQueue<byte[]> source) {
        this.source = source;
    }

    @Override
    public void sendMessage(byte[] message) {
        long n = sentCount.incrementAndGet();
        System.out.println("[SENT #" + n + "] " + message.length + " bytes over the network");
    }

    @Override
    public void start() {
        thread = new Thread(this::run, "FakeSender");
        thread.start();
    }

    @Override
    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    public long getSentCount() { return sentCount.get(); }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                sendMessage(source.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
