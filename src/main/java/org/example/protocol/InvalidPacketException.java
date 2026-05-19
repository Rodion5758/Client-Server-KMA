package org.example.protocol;

public class InvalidPacketException extends Exception {
    public InvalidPacketException(String message) {
        super(message);
    }
}
