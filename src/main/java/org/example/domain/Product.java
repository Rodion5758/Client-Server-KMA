package org.example.domain;

public class Product {
    private final int quantity;
    private final double price;

    public Product(int quantity, double price) {
        this.quantity = quantity;
        this.price = price;
    }

    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }

    public Product withQuantity(int newQuantity) {
        return new Product(newQuantity, price);
    }

    public Product withPrice(double newPrice) {
        return new Product(quantity, newPrice);
    }
}
