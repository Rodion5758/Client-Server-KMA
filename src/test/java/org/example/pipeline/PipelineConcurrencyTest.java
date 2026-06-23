package org.example.pipeline;

import org.example.PacketEncoder;
import org.example.crypto.MessageCipher;
import org.example.domain.CommandPayload;
import org.example.domain.CommandType;
import org.example.domain.ProductService;
import org.example.protocol.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PipelineConcurrencyTest {
    private static final byte[] RAW_KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    private ProductService warehouse;
    private Pipeline pipeline;
    private PacketEncoder clientEncoder;
    private FakeSender sender;

    @BeforeEach
    void setUp() {
        SecretKey key = MessageCipher.keyFrom(RAW_KEY);
        MessageCipher cipher = new MessageCipher(key);
        warehouse = new ProductService();
        pipeline = new Pipeline(warehouse, cipher);
        sender = new FakeSender(pipeline.outQueue());
        pipeline.setSender(sender);
        clientEncoder = new PacketEncoder(cipher);
        pipeline.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        pipeline.shutdown();
    }

    private byte[] encode(int cType, CommandPayload payload) throws Exception {
        return clientEncoder.encode((byte) 1, new Message<>(cType, 1, payload));
    }

    @Test
    void concurrentAddsConverge() throws Exception {
        int total = 1000;
        ExecutorService clients = Executors.newFixedThreadPool(20);
        for (int i = 0; i < total; i++) {
            clients.submit(() -> {
                byte[] pkt = encode(CommandType.ADD, new CommandPayload("grechka", null, 1, null));
                pipeline.submit(pkt);
                return null;
            });
        }
        clients.shutdown();
        assertTrue(clients.awaitTermination(15, TimeUnit.SECONDS));

        assertTrue(pipeline.awaitIdle(15_000), "pipeline must drain");
        waitForSent(total, 15_000);

        assertEquals(total, warehouse.peek("grechka").getQuantity());
        assertEquals(total, sender.getSentCount());
    }

    @Test
    void concurrentMixedAddSubtract() throws Exception {
        warehouse.add("milk", 100_000);
        int adds = 500;
        int subs = 500;
        int addQty = 10;
        int subQty = 5;

        ExecutorService clients = Executors.newFixedThreadPool(20);
        for (int i = 0; i < adds; i++) {
            clients.submit(() -> { pipeline.submit(encode(CommandType.ADD, new CommandPayload("milk", null, addQty, null))); return null; });
        }
        for (int i = 0; i < subs; i++) {
            clients.submit(() -> { pipeline.submit(encode(CommandType.SUBTRACT, new CommandPayload("milk", null, subQty, null))); return null; });
        }
        clients.shutdown();
        assertTrue(clients.awaitTermination(15, TimeUnit.SECONDS));

        assertTrue(pipeline.awaitIdle(15_000));
        waitForSent(adds + subs, 15_000);

        int expected = 100_000 + adds * addQty - subs * subQty;
        assertEquals(expected, warehouse.peek("milk").getQuantity());
    }

    @Test
    void subtractBelowZeroRejected() throws Exception {
        warehouse.add("bread", 5);
        pipeline.submit(encode(CommandType.SUBTRACT, new CommandPayload("bread", null, 10, null)));
        assertTrue(pipeline.awaitIdle(5_000));
        assertEquals(5, warehouse.peek("bread").getQuantity());
    }

    @Test
    void addGroupAndProductMembership() throws Exception {
        warehouse.addGroup("food");
        int n = 200;
        ExecutorService clients = Executors.newFixedThreadPool(10);
        String[] products = {"a", "b", "c", "d", "e"};
        for (int i = 0; i < n; i++) {
            final String p = products[i % products.length];
            clients.submit(() -> { pipeline.submit(encode(CommandType.ADD_PRODUCT_TO_GROUP, new CommandPayload(p, "food", null, null))); return null; });
        }
        clients.shutdown();
        assertTrue(clients.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(pipeline.awaitIdle(10_000));

        Set<String> members = warehouse.groupMembers("food");
        assertNotNull(members);
        assertEquals(Set.of("a", "b", "c", "d", "e"), members);
    }

    @Test
    void getQuantityReturnsCurrentValue() throws Exception {
        warehouse.add("sugar", 42);
        pipeline.submit(encode(CommandType.GET_QUANTITY, new CommandPayload("sugar", null, null, null)));
        assertTrue(pipeline.awaitIdle(5_000));
        waitForSent(1, 5_000);
        assertEquals(42, warehouse.peek("sugar").getQuantity());
    }

    @Test
    void setPriceWorksConcurrently() throws Exception {
        int n = 100;
        ExecutorService clients = Executors.newFixedThreadPool(10);
        for (int i = 0; i < n; i++) {
            final double price = i + 1.0;
            clients.submit(() -> { pipeline.submit(encode(CommandType.SET_PRICE, new CommandPayload("salt", null, null, price))); return null; });
        }
        clients.shutdown();
        assertTrue(clients.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(pipeline.awaitIdle(10_000));

        double finalPrice = warehouse.peek("salt").getPrice();
        assertTrue(finalPrice >= 1.0 && finalPrice <= n);
    }

    @Test
    void fakeReceiverDrivesFullPipeline() throws Exception {
        FakeReceiver receiver = new FakeReceiver(pipeline.rawQueue(), clientEncoder, 5);
        pipeline.setReceiver(receiver);
        receiver.start();

        Thread.sleep(500);

        pipeline.shutdown();

        assertTrue(sender.getSentCount() > 0, "FakeReceiver should have driven at least one response through the pipeline");
    }

    private void waitForSent(long expected, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            if (sender.getSentCount() >= expected) return;
            Thread.sleep(20);
        }
        fail("Sender only emitted " + sender.getSentCount() + "/" + expected + " responses before timeout");
    }
}
