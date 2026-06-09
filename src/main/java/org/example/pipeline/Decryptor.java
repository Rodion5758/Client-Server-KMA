package org.example.pipeline;

import org.example.PacketDecoder;
import org.example.domain.CommandPayload;
import org.example.protocol.Message;

import java.util.concurrent.BlockingQueue;

public class Decryptor {
    private final BlockingQueue<byte[]> inQ;
    private final BlockingQueue<Message<CommandPayload>> outQ;
    private final PacketDecoder decoder;

    private Thread thread;

    public Decryptor(BlockingQueue<byte[]> inQ, BlockingQueue<Message<CommandPayload>> outQ, PacketDecoder decoder) {
        this.inQ = inQ;
        this.outQ = outQ;
        this.decoder = decoder;
    }

    public void start() {
        thread = new Thread(this::run, "Decryptor");
        thread.start();
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    public void decrypt(byte[] message) throws Exception {
        Message<CommandPayload> decoded = decoder.decode(message, CommandPayload.class).getMessage();
        outQ.put(decoded);
    }

    private void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                decrypt(inQ.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("Decryptor dropped bad packet: " + e.getMessage());
            }
        }
    }
}
