package org.example.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwtService {
    private static final String SECRET = "s3cr3t-store-signing-key-32bytes!";

    private final Algorithm algorithm = Algorithm.HMAC256(SECRET);
    private final JWTVerifier verifier = JWT.require(algorithm).build();

    public String issue(String login) {
        return JWT.create()
                .withSubject(login)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    public String verify(String token) throws JWTVerificationException {
        return verifier.verify(token).getSubject();
    }
}
