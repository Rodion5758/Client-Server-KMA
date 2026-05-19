package org.example.protocol;

public class Packet<T> {
    private final byte bSrc;
    private final long bPktId;
    private final Message<T> message;

    public Packet(byte bSrc, long bPktId, Message<T> message) {
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.message = message;
    }

    public byte getBSrc() { return bSrc; }
    public long getBPktId() { return bPktId; }
    public Message<T> getMessage() { return message; }
}
