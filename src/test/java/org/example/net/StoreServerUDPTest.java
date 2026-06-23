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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerUDPTest {
    private static final byte[] RAW_KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    private ProductService warehouse;
    private MessageCipher cipher;
    private StoreServerUDP server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        SecretKey key = MessageCipher.keyFrom(RAW_KEY);
        cipher = new MessageCipher(key);
        warehouse = new ProductService();
        server = new StoreServerUDP(0, warehouse, cipher);
        server.start();
        port = server.getLocalPort();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    @Test
    void normalRoundTrip() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, 1, cipher)) {
            ResponsePayload r = client.send(CommandType.ADD,
                    new CommandPayload("grechka", null, 10, null));
            assertTrue(r.isOk());
            assertEquals(10, r.getQuantity());

            ResponsePayload qty = client.send(CommandType.GET_QUANTITY,
                    new CommandPayload("grechka", null, null, null));
            assertTrue(qty.isOk());
            assertEquals(10, qty.getQuantity());
        }
    }

    @Test
    void retransmitOnLostReplyAppliesCommandOnlyOnce() throws Exception {
        AtomicBoolean dropped = new AtomicBoolean(false);

        server.setDropReplyPredicate(key -> {
            if (!dropped.get()) {
                dropped.set(true);
                return true;
            }
            return false;
        });

        try (StoreClientUDP client = new StoreClientUDP("localhost", port, 2, cipher,
                500, 5)) {
            ResponsePayload r = client.send(CommandType.ADD,
                    new CommandPayload("milk", null, 7, null));
            assertTrue(r.isOk(), "Client should eventually get reply after retransmit");
            assertTrue(dropped.get(), "First reply must have been dropped");
        }

        assertEquals(7, warehouse.peek("milk").getQuantity(),
                "ADD must be applied exactly once despite retransmit");
    }

    @Test
    void subtractBelowZeroRejected() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, 3, cipher)) {
            client.send(CommandType.ADD, new CommandPayload("sugar", null, 5, null));
            ResponsePayload r = client.send(CommandType.SUBTRACT,
                    new CommandPayload("sugar", null, 100, null));
            assertFalse(r.isOk());
        }
    }

    @Test
    void multipleClientsShareProductService() throws Exception {
        try (StoreClientUDP c1 = new StoreClientUDP("localhost", port, 1, cipher);
             StoreClientUDP c2 = new StoreClientUDP("localhost", port, 2, cipher)) {
            c1.send(CommandType.ADD, new CommandPayload("bread", null, 4, null));
            c2.send(CommandType.ADD, new CommandPayload("bread", null, 6, null));
            ResponsePayload qty = c1.send(CommandType.GET_QUANTITY,
                    new CommandPayload("bread", null, null, null));
            assertTrue(qty.isOk());
            assertEquals(10, qty.getQuantity());
        }
    }
}
