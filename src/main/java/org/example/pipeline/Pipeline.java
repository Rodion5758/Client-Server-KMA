package org.example.pipeline;

import org.example.PacketDecoder;
import org.example.PacketEncoder;
import org.example.crypto.MessageCipher;
import org.example.domain.CommandPayload;
import org.example.domain.ResponsePayload;
import org.example.domain.ProductService;
import org.example.protocol.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Pipeline {
    private final BlockingQueue<byte[]> rawQ = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message<CommandPayload>> decodedQ = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message<ResponsePayload>> respQ = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> outQ = new LinkedBlockingQueue<>();

    private final Decryptor decryptor;
    private final Processor processor;
    private final Encryptor encryptor;

    private Receiver receiver;
    private Sender sender;

    public Pipeline(ProductService service, MessageCipher cipher) {
        this.decryptor = new Decryptor(rawQ, decodedQ, new PacketDecoder(cipher));
        this.processor = new Processor(decodedQ, respQ, service);
        this.encryptor = new Encryptor(respQ, outQ, new PacketEncoder(cipher), (byte) 0);
    }

    public void setReceiver(Receiver r) { this.receiver = r; }
    public void setSender(Sender s) { this.sender = s; }

    public BlockingQueue<byte[]> rawQueue() { return rawQ; }
    public BlockingQueue<byte[]> outQueue() { return outQ; }

    public void submit(byte[] rawPacket) throws InterruptedException {
        rawQ.put(rawPacket);
    }

    public void start() {
        decryptor.start();
        processor.start();
        encryptor.start();
        if (sender != null) sender.start();
        if (receiver != null) receiver.start();
    }

    public void shutdown() throws InterruptedException {
        if (receiver != null) receiver.stop();
        decryptor.stop();
        processor.stop();
        encryptor.stop();
        if (sender != null) sender.stop();
    }

    public boolean awaitIdle(long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            if (rawQ.isEmpty() && decodedQ.isEmpty() && respQ.isEmpty()) {
                Thread.sleep(20);
                if (rawQ.isEmpty() && decodedQ.isEmpty() && respQ.isEmpty()) return true;
            }
            Thread.sleep(10);
        }
        return false;
    }
}
