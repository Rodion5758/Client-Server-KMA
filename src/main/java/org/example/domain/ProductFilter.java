package org.example.domain;

public class ProductFilter {
    private String nameContains;
    private String category;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Double minPrice;
    private Double maxPrice;

    public ProductFilter nameContains(String v) { this.nameContains = v; return this; }
    public ProductFilter category(String v) { this.category = v; return this; }
    public ProductFilter minQuantity(int v) { this.minQuantity = v; return this; }
    public ProductFilter maxQuantity(int v) { this.maxQuantity = v; return this; }
    public ProductFilter minPrice(double v) { this.minPrice = v; return this; }
    public ProductFilter maxPrice(double v) { this.maxPrice = v; return this; }

    public boolean matches(Product p) {
        if (nameContains != null && !p.getName().contains(nameContains)) return false;
        if (category != null && !category.equals(p.getCategory())) return false;
        if (minQuantity != null && p.getQuantity() < minQuantity) return false;
        if (maxQuantity != null && p.getQuantity() > maxQuantity) return false;
        if (minPrice != null && p.getPrice() < minPrice) return false;
        if (maxPrice != null && p.getPrice() > maxPrice) return false;
        return true;
    }
}
