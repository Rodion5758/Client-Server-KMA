package org.example.domain;

import org.example.protocol.Message;

public class CommandHandler {
    private final ProductService service;

    public CommandHandler(ProductService service) {
        this.service = service;
    }

    public Message<ResponsePayload> handle(Message<CommandPayload> msg) {
        ResponsePayload result = dispatch(msg.getCType(), msg.getPayload());
        return new Message<>(msg.getCType(), msg.getBUserId(), result);
    }

    private ResponsePayload dispatch(int cType, CommandPayload c) {
        return switch (cType) {
            case CommandType.GET_QUANTITY         -> service.getQuantity(c.getProductName());
            case CommandType.SUBTRACT             -> service.subtract(c.getProductName(), c.getQuantity());
            case CommandType.ADD                  -> service.add(c.getProductName(), c.getQuantity());
            case CommandType.ADD_GROUP            -> service.addGroup(c.getGroupName());
            case CommandType.ADD_PRODUCT_TO_GROUP -> service.addProductToGroup(c.getGroupName(), c.getProductName());
            case CommandType.SET_PRICE            -> service.setPrice(c.getProductName(), c.getPrice());
            case CommandType.CREATE_PRODUCT       -> service.create(new Product(
                    c.getProductName(),
                    c.getCategory() != null ? c.getCategory() : "",
                    c.getQuantity() != null ? c.getQuantity() : 0,
                    c.getPrice() != null ? c.getPrice() : 0.0));
            case CommandType.UPDATE_PRODUCT       -> service.update(new Product(
                    c.getProductName(),
                    c.getCategory() != null ? c.getCategory() : "",
                    c.getQuantity() != null ? c.getQuantity() : 0,
                    c.getPrice() != null ? c.getPrice() : 0.0));
            case CommandType.DELETE_PRODUCT       -> service.delete(c.getProductName());
            default                               -> ResponsePayload.fail("Unknown command type: " + cType);
        };
    }
}
