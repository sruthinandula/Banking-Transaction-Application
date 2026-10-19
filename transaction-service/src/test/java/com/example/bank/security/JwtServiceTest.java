package com.example.bank.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void generatedTokenRoundTripsToUsername() {
        JwtService jwtService = new JwtService(SECRET, 60);

        String token = jwtService.generateToken("alice");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60);
        String token = jwtService.generateToken("alice");

        assertThrows(JwtException.class, () -> jwtService.extractUsername(token + "x"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtService("ffffffffffffffffffffffffffffffff", 60).generateToken("alice");

        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60).extractUsername(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, -1);
        String token = jwtService.generateToken("alice");

        assertThrows(JwtException.class, () -> jwtService.extractUsername(token));
    }
}
