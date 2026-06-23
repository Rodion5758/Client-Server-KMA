package org.example.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService();
    }

    @Test
    void createSuccess() {
        ResponsePayload r = service.create(new Product("milk", "dairy", 10, 2.5));
        assertTrue(r.isOk());
        Product p = service.read("milk");
        assertNotNull(p);
        assertEquals("dairy", p.getCategory());
        assertEquals(10, p.getQuantity());
        assertEquals(2.5, p.getPrice());
    }

    @Test
    void createDuplicateFails() {
        service.create(new Product("milk", "dairy", 10, 2.5));
        ResponsePayload r = service.create(new Product("milk", "dairy", 5, 1.0));
        assertFalse(r.isOk(), "Duplicate create must fail");
        assertEquals(10, service.read("milk").getQuantity(), "Original must be unchanged");
    }

    @Test
    void readMissingReturnsNull() {
        assertNull(service.read("nonexistent"));
    }

    @Test
    void updateSuccess() {
        service.create(new Product("bread", "bakery", 5, 1.0));
        ResponsePayload r = service.update(new Product("bread", "bakery", 20, 3.5));
        assertTrue(r.isOk());
        Product p = service.read("bread");
        assertEquals(20, p.getQuantity());
        assertEquals(3.5, p.getPrice());
    }

    @Test
    void updateMissingFails() {
        ResponsePayload r = service.update(new Product("ghost", "none", 1, 1.0));
        assertFalse(r.isOk());
    }

    @Test
    void deleteSuccess() {
        service.create(new Product("sugar", "sweet", 100, 0.5));
        ResponsePayload r = service.delete("sugar");
        assertTrue(r.isOk());
        assertNull(service.read("sugar"));
    }

    @Test
    void deleteMissingFails() {
        ResponsePayload r = service.delete("ghost");
        assertFalse(r.isOk());
    }

    private void seedProducts() {
        service.create(new Product("apple",  "fruit",  50,  1.0));
        service.create(new Product("banana", "fruit",  30,  0.8));
        service.create(new Product("milk",   "dairy",  20,  2.0));
        service.create(new Product("cheese", "dairy",   5, 10.0));
        service.create(new Product("water",  "drinks", 200,  0.5));
    }

    @Test
    void searchByNameSubstring() {
        seedProducts();
        List<Product> result = service.search(new ProductFilter().nameContains("e"), 0, 10);
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(p -> p.getName().equals("apple")));
        assertTrue(result.stream().anyMatch(p -> p.getName().equals("cheese")));
        assertTrue(result.stream().anyMatch(p -> p.getName().equals("water")));
    }

    @Test
    void searchByCategory() {
        seedProducts();
        List<Product> result = service.search(new ProductFilter().category("dairy"), 0, 10);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getCategory().equals("dairy")));
    }

    @Test
    void searchByMinPriceOnly() {
        seedProducts();
        List<Product> result = service.search(new ProductFilter().minPrice(3.0), 0, 10);
        assertEquals(1, result.size());
        assertEquals("cheese", result.getFirst().getName());
    }

    @Test
    void searchByQuantityRange() {
        seedProducts();
        List<Product> result = service.search(new ProductFilter().minQuantity(25).maxQuantity(60), 0, 10);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.getName().equals("apple")));
        assertTrue(result.stream().anyMatch(p -> p.getName().equals("banana")));
    }

    @Test
    void searchCombinedCategoryAndMinPrice() {
        seedProducts();
        List<Product> result = service.search(
                new ProductFilter().category("dairy").minPrice(5.0), 0, 10);
        assertEquals(1, result.size());
        assertEquals("cheese", result.getFirst().getName());
    }

    @Test
    void searchEmptyFilterReturnsAll() {
        seedProducts();
        List<Product> result = service.search(new ProductFilter(), 0, 100);
        assertEquals(5, result.size());
    }

    @Test
    void paginationFirstPage() {
        seedProducts();
        List<Product> page0 = service.search(new ProductFilter(), 0, 2);
        assertEquals(2, page0.size());
        assertEquals("apple", page0.get(0).getName());
        assertEquals("banana", page0.get(1).getName());
    }

    @Test
    void paginationSecondPage() {
        seedProducts();
        List<Product> page1 = service.search(new ProductFilter(), 1, 2);
        assertEquals(2, page1.size());
        assertEquals("cheese", page1.get(0).getName());
        assertEquals("milk", page1.get(1).getName());
    }

    @Test
    void paginationLastPartialPage() {
        seedProducts();
        List<Product> page2 = service.search(new ProductFilter(), 2, 2);
        assertEquals(1, page2.size());
        assertEquals("water", page2.getFirst().getName());
    }

    @Test
    void paginationOutOfRangeReturnsEmpty() {
        seedProducts();
        List<Product> page = service.search(new ProductFilter(), 99, 10);
        assertTrue(page.isEmpty());
    }
}
