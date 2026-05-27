package org.example.domain;

public final class CommandType {
    public static final int GET_QUANTITY = 1;
    public static final int SUBTRACT = 2;
    public static final int ADD = 3;
    public static final int ADD_GROUP = 4;
    public static final int ADD_PRODUCT_TO_GROUP = 5;
    public static final int SET_PRICE = 6;

    private CommandType() {}
}
