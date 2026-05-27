package org.example.pipeline;

public interface Sender {
    void sendMessage(byte[] message);
    void start();
    void stop() throws InterruptedException;
}
