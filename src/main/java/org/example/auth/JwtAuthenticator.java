package org.example.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class JwtAuthenticator extends Authenticator {
    private static final String REALM = "store";

    private final JwtService jwtService;

    public JwtAuthenticator(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return new Failure(401);
        }
        String token = header.substring(7);
        try {
            String login = jwtService.verify(token);
            return new Success(new HttpPrincipal(login, REALM));
        } catch (JWTVerificationException e) {
            return new Failure(401);
        }
    }
}
