package com.devrick.pos.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.security.principal.AuthenticatedUser;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

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
        assertEquals(Optional.of(TENANT_ID), jwtService.extractTenantId(token));
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
        assertEquals(Optional.of(TENANT_ID), jwtService.extractTenantId(token));
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

    @Test
    void isTokenValidRejectsTenantMismatch() {
        JwtService jwtService =
                new JwtService(jwtProperties, fixedClock, new com.fasterxml.jackson.databind.ObjectMapper());
        String token = jwtService.generateAccessToken(userDetails());

        assertFalse(jwtService.isTokenValid(token, otherTenantUserDetails()));
    }

    private UserDetails userDetails() {
        return new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                TENANT_ID,
                "Default Business",
                "DEFAULT",
                TenantStatus.ACTIVE,
                "john.doe@example.com",
                "encoded-password",
                true,
                false,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private UserDetails otherTenantUserDetails() {
        return new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "Another Business",
                "ANOTHER",
                TenantStatus.ACTIVE,
                "john.doe@example.com",
                "encoded-password",
                true,
                false,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
