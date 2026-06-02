package org.example.domain;

import org.example.protocol.Message;

public class CommandHandler {
    private final Warehouse warehouse;

    public CommandHandler(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Message<ResponsePayload> handle(Message<CommandPayload> msg) {
        ResponsePayload result = dispatch(msg.getCType(), msg.getPayload());
        return new Message<>(msg.getCType(), msg.getBUserId(), result);
    }

    private ResponsePayload dispatch(int cType, CommandPayload c) {
        return switch (cType) {
            case CommandType.GET_QUANTITY          -> warehouse.getQuantity(c.getProductName());
            case CommandType.SUBTRACT              -> warehouse.subtract(c.getProductName(), c.getQuantity());
            case CommandType.ADD                   -> warehouse.add(c.getProductName(), c.getQuantity());
            case CommandType.ADD_GROUP             -> warehouse.addGroup(c.getGroupName());
            case CommandType.ADD_PRODUCT_TO_GROUP  -> warehouse.addProductToGroup(c.getGroupName(), c.getProductName());
            case CommandType.SET_PRICE             -> warehouse.setPrice(c.getProductName(), c.getPrice());
            default                                -> ResponsePayload.fail("Unknown command type: " + cType);
        };
    }
}
