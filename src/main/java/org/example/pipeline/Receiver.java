package org.example.pipeline;

public interface Receiver {
    void receiveMessage();
    void start();
    void stop() throws InterruptedException;
}
