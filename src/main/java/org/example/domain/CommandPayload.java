package org.example.domain;

public class CommandPayload {
    private String productName;
    private String groupName;
    private String category;
    private Integer quantity;
    private Double price;

    public CommandPayload() {}

    public CommandPayload(String productName, String groupName, Integer quantity, Double price) {
        this.productName = productName;
        this.groupName = groupName;
        this.quantity = quantity;
        this.price = price;
    }

    public CommandPayload(String productName, String groupName, String category, Integer quantity, Double price) {
        this.productName = productName;
        this.groupName = groupName;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductName() { return productName; }
    public String getGroupName() { return groupName; }
    public String getCategory() { return category; }
    public Integer getQuantity() { return quantity; }
    public Double getPrice() { return price; }
}
