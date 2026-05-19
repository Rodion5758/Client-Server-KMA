package org.example;

import java.util.Objects;

public class ExamplePayload {
    private String text;
    private long timestamp;

    public ExamplePayload() {}

    public ExamplePayload(String text, long timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamplePayload that)) return false;
        return timestamp == that.timestamp && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() { return Objects.hash(text, timestamp); }
}
