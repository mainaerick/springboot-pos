package com.devrick.pos.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private JwtProperties jwtProperties;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("pos-api");
        jwtProperties.setSecret("pos-test-secret-key-pos-test-secret-key");
        jwtProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
        fixedClock = Clock.fixed(Instant.parse("2026-08-02T10:15:30Z"), ZoneOffset.UTC);
    }

    @Test
    void generateAccessTokenCreatesReadableToken() {
        JwtService jwtService =
                new JwtService(jwtProperties, fixedClock, new com.fasterxml.jackson.databind.ObjectMapper());

        String token = jwtService.generateAccessToken(userDetails());

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertEquals("john.doe@example.com", jwtService.extractUsername(token));
        assertEquals(Instant.parse("2026-08-02T10:30:30Z"), jwtService.extractExpiration(token));
    }

    @Test
    void generateRefreshTokenCreatesReadableToken() {
        JwtService jwtService =
                new JwtService(jwtProperties, fixedClock, new com.fasterxml.jackson.databind.ObjectMapper());

        String token = jwtService.generateRefreshToken(userDetails());

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertEquals("john.doe@example.com", jwtService.extractUsername(token));
        assertEquals(Instant.parse("2026-08-09T10:15:30Z"), jwtService.extractExpiration(token));
    }

    @Test
    void isTokenValidAcceptsMatchingUser() {
        JwtService jwtService =
                new JwtService(jwtProperties, fixedClock, new com.fasterxml.jackson.databind.ObjectMapper());

        String token = jwtService.generateAccessToken(userDetails());

        assertTrue(jwtService.isTokenValid(token, userDetails()));
    }

    @Test
    void isRefreshTokenValidAcceptsMatchingUser() {
        JwtService jwtService =
                new JwtService(jwtProperties, fixedClock, new com.fasterxml.jackson.databind.ObjectMapper());

        String token = jwtService.generateRefreshToken(userDetails());

        assertTrue(jwtService.isRefreshTokenValid(token, userDetails()));
        assertFalse(jwtService.isTokenValid(token, userDetails()));
    }

    @Test
    void isTokenValidRejectsExpiredToken() {
        JwtService issuingService =
                new JwtService(jwtProperties, fixedClock, new com.fasterxml.jackson.databind.ObjectMapper());
        String token = issuingService.generateAccessToken(userDetails());

        Clock expiredClock = Clock.fixed(Instant.parse("2026-08-02T10:45:31Z"), ZoneOffset.UTC);
        JwtService validatingService =
                new JwtService(jwtProperties, expiredClock, new com.fasterxml.jackson.databind.ObjectMapper());

        assertFalse(validatingService.isTokenValid(token, userDetails()));
    }

    @Test
    void isTokenValidRejectsTamperedSignature() {
        JwtService jwtService =
                new JwtService(jwtProperties, fixedClock, new com.fasterxml.jackson.databind.ObjectMapper());
        String token = jwtService.generateAccessToken(userDetails());
        String tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertFalse(jwtService.isTokenValid(tamperedToken, userDetails()));
        assertThrows(RuntimeException.class, () -> jwtService.extractUsername(tamperedToken));
    }

    private UserDetails userDetails() {
        return new org.springframework.security.core.userdetails.User(
                "john.doe@example.com",
                "encoded-password",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
