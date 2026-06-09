package org.example.domain;

public final class CommandType {
    public static final int GET_QUANTITY = 1;
    public static final int SUBTRACT = 2;
    public static final int ADD = 3;
    public static final int ADD_GROUP = 4;
    public static final int ADD_PRODUCT_TO_GROUP = 5;
    public static final int SET_PRICE = 6;
    public static final int CREATE_PRODUCT = 7;
    public static final int UPDATE_PRODUCT = 8;
    public static final int DELETE_PRODUCT = 9;

    private CommandType() {}
}
