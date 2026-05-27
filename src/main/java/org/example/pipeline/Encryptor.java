package org.example.pipeline;

import org.example.PacketEncoder;
import org.example.domain.ResponsePayload;
import org.example.protocol.Message;

import java.util.concurrent.BlockingQueue;

public class Encryptor {
    private final BlockingQueue<Message<ResponsePayload>> inQ;
    private final BlockingQueue<byte[]> outQ;
    private final PacketEncoder encoder;
    private final byte serverSrc;

    private Thread thread;

    public Encryptor(BlockingQueue<Message<ResponsePayload>> inQ,
                     BlockingQueue<byte[]> outQ,
                     PacketEncoder encoder,
                     byte serverSrc) {
        this.inQ = inQ;
        this.outQ = outQ;
        this.encoder = encoder;
        this.serverSrc = serverSrc;
    }

    public void start() {
        thread = new Thread(this::run, "Encryptor");
        thread.start();
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    public byte[] encrypt(Message<ResponsePayload> message) throws Exception {
        return encoder.encode(serverSrc, message);
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                outQ.put(encrypt(inQ.take()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("Encryptor error: " + e.getMessage());
            }
        }
    }
}
