package org.example.net;

import org.example.crypto.MessageCipher;
import org.example.domain.CommandPayload;
import org.example.domain.CommandType;
import org.example.domain.ResponsePayload;
import org.example.domain.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerTCPTest {
    private static final byte[] RAW_KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    private ProductService warehouse;
    private MessageCipher cipher;
    private StoreServerTCP server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        SecretKey key = MessageCipher.keyFrom(RAW_KEY);
        cipher = new MessageCipher(key);
        warehouse = new ProductService();
        server = new StoreServerTCP(0, warehouse, cipher);
        server.start();
        port = server.getLocalPort();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    @Test
    void addAndGetQuantity() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, 1, cipher)) {
            ResponsePayload r = client.send(CommandType.ADD,
                    new CommandPayload("milk", null, 10, null));
            assertTrue(r.isOk());
            assertEquals(10, r.getQuantity());

            ResponsePayload qty = client.send(CommandType.GET_QUANTITY,
                    new CommandPayload("milk", null, null, null));
            assertTrue(qty.isOk());
            assertEquals(10, qty.getQuantity());
        }
    }

    @Test
    void concurrentAddsConverge() throws Exception {
        int threads = 20;
        int addsPerThread = 50;
        int delta = 1;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int userId = i;
            futures.add(pool.submit(() -> {
                try (StoreClientTCP client = new StoreClientTCP("localhost", port, userId, cipher)) {
                    start.await();
                    for (int j = 0; j < addsPerThread; j++) {
                        ResponsePayload r = client.send(CommandType.ADD,
                                new CommandPayload("grechka", null, delta, null));
                        assertTrue(r.isOk());
                    }
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<Void> f : futures) f.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        int expected = threads * addsPerThread * delta;
        assertEquals(expected, warehouse.peek("grechka").getQuantity());
    }

    @Test
    void subtractBelowZeroRejected() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, 2, cipher)) {
            client.send(CommandType.ADD, new CommandPayload("sugar", null, 5, null));
            ResponsePayload r = client.send(CommandType.SUBTRACT,
                    new CommandPayload("sugar", null, 100, null));
            assertFalse(r.isOk());
        }
    }

    @Test
    void multipleClientsShareProductService() throws Exception {
        try (StoreClientTCP c1 = new StoreClientTCP("localhost", port, 1, cipher);
             StoreClientTCP c2 = new StoreClientTCP("localhost", port, 2, cipher)) {
            c1.send(CommandType.ADD, new CommandPayload("bread", null, 7, null));
            c2.send(CommandType.ADD, new CommandPayload("bread", null, 3, null));
            ResponsePayload qty = c1.send(CommandType.GET_QUANTITY,
                    new CommandPayload("bread", null, null, null));
            assertTrue(qty.isOk());
            assertEquals(10, qty.getQuantity());
        }
    }
}
