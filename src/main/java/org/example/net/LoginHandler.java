package org.example.net;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.auth.JwtService;
import org.example.auth.UserService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class LoginHandler implements HttpHandler {
    private final UserService userService;
    private final JwtService jwtService;
    private final Gson gson = new Gson();

    LoginHandler(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "");
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            LoginRequest req = gson.fromJson(reader, LoginRequest.class);
            if (req == null || !userService.authenticate(req.login, req.password)) {
                sendResponse(exchange, 401, "");
                return;
            }
            String token = jwtService.issue(req.login);
            String body = gson.toJson(new TokenResponse(token));
            sendResponse(exchange, 200, body);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.getResponseBody().close();
    }
}
