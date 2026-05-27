package org.example.domain;

public class ResponsePayload {
    private boolean ok;
    private String message;
    private Integer quantity;

    public ResponsePayload(boolean ok, String message, Integer quantity) {
        this.ok = ok;
        this.message = message;
        this.quantity = quantity;
    }

    public static ResponsePayload ok() {
        return new ResponsePayload(true, "OK", null);
    }

    public static ResponsePayload ok(int quantity) {
        return new ResponsePayload(true, "OK", quantity);
    }

    public static ResponsePayload fail(String message) {
        return new ResponsePayload(false, message, null);
    }

    public boolean isOk() { return ok; }
    public String getMessage() { return message; }
    public Integer getQuantity() { return quantity; }
}
