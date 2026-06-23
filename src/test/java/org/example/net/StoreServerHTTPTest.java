package org.example.net;

import org.example.auth.JwtService;
import org.example.auth.UserService;
import org.example.domain.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerHTTPTest {
    private StoreServerHTTP server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = new StoreServerHTTP(0, new ProductService(), new UserService(), new JwtService());
        server.start();
        baseUrl = "http://localhost:" + server.getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private String fetchToken() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"login\":\"admin\",\"password\":\"admin\"}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        String body = res.body();
        int start = body.indexOf(":\"") + 2;
        int end = body.lastIndexOf("\"");
        return body.substring(start, end);
    }

    @Test
    void loginReturnsToken() throws Exception {
        String token = fetchToken();
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void loginRejectsBadCredentials() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"login\":\"admin\",\"password\":\"wrong\"}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, res.statusCode());
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/milk"))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, res.statusCode());
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/milk"))
                .GET()
                .header("Authorization", "Bearer not.a.real.token")
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, res.statusCode());
    }

    @Test
    void createProductSucceeds() throws Exception {
        String token = fetchToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products"))
                .PUT(HttpRequest.BodyPublishers.ofString("{\"name\":\"milk\",\"category\":\"dairy\",\"quantity\":10,\"price\":2.5}"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res.statusCode());
    }

    @Test
    void getProductReturnsIt() throws Exception {
        String token = fetchToken();
        HttpRequest put = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products"))
                .PUT(HttpRequest.BodyPublishers.ofString("{\"name\":\"apple\",\"category\":\"fruit\",\"quantity\":5,\"price\":1.0}"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();
        client.send(put, HttpResponse.BodyHandlers.ofString());

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/apple"))
                .GET()
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> res = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"name\":\"apple\""), "Response should contain product name");
        assertTrue(res.body().contains("\"category\":\"fruit\""), "Response should contain category");
    }

    @Test
    void createDuplicateConflicts() throws Exception {
        String token = fetchToken();
        String body = "{\"name\":\"bread\",\"category\":\"bakery\",\"quantity\":3,\"price\":1.5}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products"))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> dup = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, dup.statusCode());
    }

    @Test
    void getMissingReturns404() throws Exception {
        String token = fetchToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/nonexistent"))
                .GET()
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
    }

    @Test
    void updateProductSucceeds() throws Exception {
        String token = fetchToken();
        HttpRequest create = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products"))
                .PUT(HttpRequest.BodyPublishers.ofString("{\"name\":\"juice\",\"category\":\"drinks\",\"quantity\":4,\"price\":3.0}"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();
        client.send(create, HttpResponse.BodyHandlers.ofString());

        HttpRequest update = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/juice"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"category\":\"beverages\",\"quantity\":8,\"price\":3.5}"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> res = client.send(update, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
    }

    @Test
    void updateMissingReturns404() throws Exception {
        String token = fetchToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/ghost"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"quantity\":1}"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
    }

    @Test
    void deleteProductSucceeds() throws Exception {
        String token = fetchToken();
        HttpRequest create = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products"))
                .PUT(HttpRequest.BodyPublishers.ofString("{\"name\":\"water\",\"category\":\"drinks\",\"quantity\":20,\"price\":0.5}"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .build();
        client.send(create, HttpResponse.BodyHandlers.ofString());

        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/water"))
                .DELETE()
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> res = client.send(delete, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, res.statusCode());
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        String token = fetchToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/phantom"))
                .DELETE()
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
    }
}
