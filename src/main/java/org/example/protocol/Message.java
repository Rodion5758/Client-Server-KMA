package org.example.protocol;

public class Message<T> {
    private final int cType;
    private final int bUserId;
    private final T payload;

    public Message(int cType, int bUserId, T payload) {
        this.cType = cType;
        this.bUserId = bUserId;
        this.payload = payload;
    }

    public int getCType() { return cType; }
    public int getBUserId() { return bUserId; }
    public T getPayload() { return payload; }
}
