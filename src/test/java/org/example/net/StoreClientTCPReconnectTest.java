package org.example.net;

import org.example.crypto.MessageCipher;
import org.example.domain.CommandPayload;
import org.example.domain.CommandType;
import org.example.domain.ResponsePayload;
import org.example.domain.ProductService;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StoreClientTCPReconnectTest {
    private static final byte[] RAW_KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Test
    void clientReconnectsAfterServerRestart() throws Exception {
        SecretKey key = MessageCipher.keyFrom(RAW_KEY);
        MessageCipher cipher = new MessageCipher(key);
        ProductService warehouse = new ProductService();

        StoreServerTCP server = new StoreServerTCP(0, warehouse, cipher);
        server.start();
        int port = server.getLocalPort();

        try (StoreClientTCP client = new StoreClientTCP("localhost", port, 1, cipher)) {
            ResponsePayload r1 = client.send(CommandType.ADD,
                    new CommandPayload("milk", null, 5, null));
            assertTrue(r1.isOk(), "First send should succeed");

            server.stop();

            AtomicReference<ResponsePayload> result = new AtomicReference<>();
            AtomicReference<Exception> error = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);

            Thread sendThread = new Thread(() -> {
                try {
                    result.set(client.send(CommandType.ADD,
                            new CommandPayload("milk", null, 3, null)));
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    done.countDown();
                }
            });
            sendThread.start();

            Thread.sleep(800);

            StoreServerTCP server2 = new StoreServerTCP(port, warehouse, cipher);
            server2.start();

            assertTrue(done.await(10, TimeUnit.SECONDS), "Client should reconnect within 10s");
            server2.stop();

            assertNull(error.get(), "Client should not throw: " + error.get());
            assertNotNull(result.get());
            assertTrue(result.get().isOk(), "Request after reconnect should succeed");
            assertEquals(8, warehouse.peek("milk").getQuantity(),
                    "Both ADDs must have been applied exactly once");
        }
    }
}
