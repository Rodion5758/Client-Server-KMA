package org.example.net;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.domain.Product;
import org.example.domain.ProductService;
import org.example.domain.ResponsePayload;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class ProductHandler implements HttpHandler {
    private final ProductService productService;
    private final Gson gson = new Gson();

    ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String tail = path.replaceFirst("^/products/?", "");
        boolean hasId = !tail.isEmpty();

        switch (method) {
            case "GET" -> {
                if (!hasId) { sendEmpty(exchange, 400); return; }
                handleGet(exchange, tail);
            }
            case "PUT" -> {
                if (hasId) { sendEmpty(exchange, 400); return; }
                handleCreate(exchange);
            }
            case "POST" -> {
                if (!hasId) { sendEmpty(exchange, 400); return; }
                handleUpdate(exchange, tail);
            }
            case "DELETE" -> {
                if (!hasId) { sendEmpty(exchange, 400); return; }
                handleDelete(exchange, tail);
            }
            default -> sendEmpty(exchange, 405);
        }
    }

    private void handleGet(HttpExchange exchange, String name) throws IOException {
        Product p = productService.read(name);
        if (p == null) {
            sendEmpty(exchange, 404);
        } else {
            sendJson(exchange, 200, gson.toJson(p));
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        ProductRequest req = parseBody(exchange);
        if (req == null || req.name == null) { sendEmpty(exchange, 400); return; }
        Product p = toProduct(req.name, req);
        ResponsePayload r = productService.create(p);
        if (r.isOk()) {
            sendEmpty(exchange, 201);
        } else {
            sendJson(exchange, 409, gson.toJson(r));
        }
    }

    private void handleUpdate(HttpExchange exchange, String name) throws IOException {
        ProductRequest req = parseBody(exchange);
        if (req == null) { sendEmpty(exchange, 400); return; }
        Product p = toProduct(name, req);
        ResponsePayload r = productService.update(p);
        if (r.isOk()) {
            sendEmpty(exchange, 200);
        } else {
            sendJson(exchange, 404, gson.toJson(r));
        }
    }

    private void handleDelete(HttpExchange exchange, String name) throws IOException {
        ResponsePayload r = productService.delete(name);
        if (r.isOk()) {
            sendEmpty(exchange, 204);
        } else {
            sendEmpty(exchange, 404);
        }
    }

    private ProductRequest parseBody(HttpExchange exchange) {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, ProductRequest.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Product toProduct(String name, ProductRequest req) {
        return new Product(
                name,
                req.category != null ? req.category : "",
                req.quantity != null ? req.quantity : 0,
                req.price != null ? req.price : 0.0
        );
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.getResponseBody().close();
    }
}
