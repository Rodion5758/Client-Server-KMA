package org.example.domain;

import org.example.protocol.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandHandlerProductTest {
    private ProductService service;
    private CommandHandler handler;

    @BeforeEach
    void setUp() {
        service = new ProductService();
        handler = new CommandHandler(service);
    }

    private Message<ResponsePayload> send(int cType, CommandPayload payload) {
        return handler.handle(new Message<>(cType, 1, payload));
    }

    @Test
    void createProductViaCommand() {
        Message<ResponsePayload> resp = send(CommandType.CREATE_PRODUCT,
                new CommandPayload("milk", null, "dairy", 10, 2.5));
        assertTrue(resp.getPayload().isOk());
        Product p = service.read("milk");
        assertNotNull(p);
        assertEquals("dairy", p.getCategory());
        assertEquals(10, p.getQuantity());
    }

    @Test
    void createDuplicateViaCommandFails() {
        send(CommandType.CREATE_PRODUCT, new CommandPayload("milk", null, "dairy", 10, 2.5));
        Message<ResponsePayload> resp = send(CommandType.CREATE_PRODUCT,
                new CommandPayload("milk", null, "dairy", 5, 1.0));
        assertFalse(resp.getPayload().isOk());
    }

    @Test
    void updateProductViaCommand() {
        service.create(new Product("bread", "bakery", 5, 1.0));
        Message<ResponsePayload> resp = send(CommandType.UPDATE_PRODUCT,
                new CommandPayload("bread", null, "bakery", 20, 3.5));
        assertTrue(resp.getPayload().isOk());
        assertEquals(20, service.read("bread").getQuantity());
        assertEquals(3.5, service.read("bread").getPrice());
    }

    @Test
    void updateMissingProductViaCommandFails() {
        Message<ResponsePayload> resp = send(CommandType.UPDATE_PRODUCT,
                new CommandPayload("ghost", null, "none", 1, 1.0));
        assertFalse(resp.getPayload().isOk());
    }

    @Test
    void deleteProductViaCommand() {
        service.create(new Product("sugar", "sweet", 100, 0.5));
        Message<ResponsePayload> resp = send(CommandType.DELETE_PRODUCT,
                new CommandPayload("sugar", null, null, null));
        assertTrue(resp.getPayload().isOk());
        assertNull(service.read("sugar"));
    }

    @Test
    void deleteMissingProductViaCommandFails() {
        Message<ResponsePayload> resp = send(CommandType.DELETE_PRODUCT,
                new CommandPayload("ghost", null, null, null));
        assertFalse(resp.getPayload().isOk());
    }

    @Test
    void legacyAddCommandStillWorks() {
        Message<ResponsePayload> resp = send(CommandType.ADD,
                new CommandPayload("grechka", null, 42, null));
        assertTrue(resp.getPayload().isOk());
        assertEquals(42, resp.getPayload().getQuantity());
    }

    @Test
    void cTypePreservedInResponse() {
        Message<ResponsePayload> resp = send(CommandType.CREATE_PRODUCT,
                new CommandPayload("item", null, "cat", 1, 1.0));
        assertEquals(CommandType.CREATE_PRODUCT, resp.getCType());
    }
}
