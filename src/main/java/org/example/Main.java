package org.example;

import org.example.crypto.MessageCipher;
import org.example.domain.Warehouse;
import org.example.pipeline.FakeReceiver;
import org.example.pipeline.FakeSender;
import org.example.pipeline.Pipeline;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        byte[] rawKey = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        SecretKey key = MessageCipher.keyFrom(rawKey);
        MessageCipher cipher = new MessageCipher(key);

        Warehouse warehouse = new Warehouse();
        Pipeline pipeline = new Pipeline(warehouse, cipher);

        PacketEncoder encoder = new PacketEncoder(cipher);
        FakeReceiver receiver = new FakeReceiver(pipeline.rawQueue(), encoder, 100);
        FakeSender sender = new FakeSender(pipeline.outQueue());

        pipeline.setReceiver(receiver);
        pipeline.setSender(sender);
        pipeline.start();

        System.out.println("Pipeline running for 5 seconds...");
        Thread.sleep(5000);

        pipeline.shutdown();
        System.out.println("Pipeline stopped. Total responses sent: " + sender.getSentCount());
    }
}
