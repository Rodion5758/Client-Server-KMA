package org.example.domain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Warehouse {
    private final ConcurrentHashMap<String, Product> products = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> groups = new ConcurrentHashMap<>();

    public ResponsePayload getQuantity(String name) {
        Product p = products.get(name);
        if (p == null) return ResponsePayload.fail("Unknown product: " + name);
        return ResponsePayload.ok(p.getQuantity());
    }

    public ResponsePayload add(String name, int delta) {
        if (delta < 0) return ResponsePayload.fail("Negative delta");
        Product updated = products.compute(name, (k, current) -> {
            if (current == null) return new Product(delta, 0.0);
            return current.withQuantity(current.getQuantity() + delta);
        });
        return ResponsePayload.ok(updated.getQuantity());
    }

    public ResponsePayload subtract(String name, int delta) {
        if (delta < 0) return ResponsePayload.fail("Negative delta");
        AtomicBoolean insufficient = new AtomicBoolean(false);
        Product updated = products.compute(name, (k, current) -> {
            if (current == null || current.getQuantity() < delta) {
                insufficient.set(true);
                return current;
            }
            return current.withQuantity(current.getQuantity() - delta);
        });
        if (insufficient.get()) return ResponsePayload.fail("Insufficient quantity for " + name);
        return ResponsePayload.ok(updated.getQuantity());
    }

    public ResponsePayload addGroup(String name) {
        Set<String> existing = groups.putIfAbsent(name, ConcurrentHashMap.newKeySet());
        if (existing != null) return ResponsePayload.fail("Group already exists: " + name);
        return ResponsePayload.ok();
    }

    public ResponsePayload addProductToGroup(String groupName, String productName) {
        Set<String> members = groups.get(groupName);
        if (members == null) return ResponsePayload.fail("Unknown group: " + groupName);
        products.computeIfAbsent(productName, n -> new Product(0, 0.0));
        members.add(productName);
        return ResponsePayload.ok();
    }

    public ResponsePayload setPrice(String name, double price) {
        if (price < 0) return ResponsePayload.fail("Negative price");
        products.compute(name, (k, current) -> {
            if (current == null) return new Product(0, price);
            return current.withPrice(price);
        });
        return ResponsePayload.ok();
    }

    public Product peek(String name) { return products.get(name); }
    public Set<String> groupMembers(String groupName) { return groups.get(groupName); }
}
