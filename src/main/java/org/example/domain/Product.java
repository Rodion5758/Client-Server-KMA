package org.example.domain;

public class Product {
    private final String name;
    private final String category;
    private final int quantity;
    private final double price;

    public Product(String name, String category, int quantity, double price) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }

    public Product withQuantity(int newQuantity) {
        return new Product(name, category, newQuantity, price);
    }

    public Product withPrice(double newPrice) {
        return new Product(name, category, quantity, newPrice);
    }
}
