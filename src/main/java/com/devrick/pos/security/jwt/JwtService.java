package com.devrick.pos.security.jwt;

import com.devrick.pos.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String JWT_HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(
                userDetails.getUsername(),
                TokenType.ACCESS,
                jwtProperties.getAccessTokenExpiration(),
                extractTenantId(userDetails));
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(
                userDetails.getUsername(),
                TokenType.REFRESH,
                jwtProperties.getRefreshTokenExpiration(),
                extractTenantId(userDetails));
    }

    public String generateAccessToken(String subject) {
        return generateToken(subject, TokenType.ACCESS, jwtProperties.getAccessTokenExpiration(), Optional.empty());
    }

    public Optional<UUID> extractTenantId(String token) {
        String tenantId = parseToken(token).tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return Optional.empty();
        }

        return Optional.of(UUID.fromString(tenantId));
    }

    private String generateToken(
            String subject, TokenType tokenType, java.time.Duration expiration, Optional<UUID> tenantId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(expiration);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", subject);
        claims.put("iss", jwtProperties.getIssuer());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("typ", tokenType.getClaimValue());
        tenantId.ifPresent(value -> claims.put("tenantId", value.toString()));
        if (claims.isEmpty()) {
            throw new IllegalStateException("JWT claims cannot be empty");
        }

        String encodedHeader = base64UrlEncode(JWT_HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(writeValueAsBytes(claims));
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public String extractUsername(String token) {
        return parseToken(token).subject();
    }

    public Instant extractExpiration(String token) {
        return parseToken(token).expiresAt();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            JwtToken tokenData = parseToken(token);
            return tokenData.subject().equals(userDetails.getUsername())
                    && tokenData.issuer().equals(jwtProperties.getIssuer())
                    && TokenType.ACCESS.getClaimValue().equals(tokenData.type())
                    && tokenData.expiresAt().isAfter(clock.instant())
                    && tenantMatches(tokenData.tenantId(), userDetails)
                    && userDetails.isEnabled();
        } catch (JwtValidationException exception) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            JwtToken tokenData = parseToken(token);
            return tokenData.subject().equals(userDetails.getUsername())
                    && tokenData.issuer().equals(jwtProperties.getIssuer())
                    && TokenType.REFRESH.getClaimValue().equals(tokenData.type())
                    && tokenData.expiresAt().isAfter(clock.instant())
                    && tenantMatches(tokenData.tenantId(), userDetails)
                    && userDetails.isEnabled();
        } catch (JwtValidationException exception) {
            return false;
        }
    }

    private JwtToken parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtValidationException("JWT must contain three parts");
        }

        String headerJson = decodeToString(parts[0]);
        String payloadJson = decodeToString(parts[1]);

        verifySignature(parts[0] + "." + parts[1], parts[2]);

        JsonNode headerNode = readTree(headerJson);
        JsonNode payloadNode = readTree(payloadJson);

        String algorithm = requiredText(headerNode, "alg");
        String type = requiredText(headerNode, "typ");
        if (!"HS256".equals(algorithm) || !"JWT".equals(type)) {
            throw new JwtValidationException("Unsupported JWT header");
        }

        String subject = requiredText(payloadNode, "sub");
        String issuer = requiredText(payloadNode, "iss");
        long issuedAtEpochSecond = requiredLong(payloadNode, "iat");
        long expiresAtEpochSecond = requiredLong(payloadNode, "exp");
        String tokenType = requiredText(payloadNode, "typ");
        String tenantId = optionalText(payloadNode, "tenantId");

        return new JwtToken(
                subject,
                issuer,
                Instant.ofEpochSecond(issuedAtEpochSecond),
                Instant.ofEpochSecond(expiresAtEpochSecond),
                tokenType,
                tenantId);
    }

    private boolean tenantMatches(String tokenTenantId, UserDetails userDetails) {
        if (!StringUtils.hasText(tokenTenantId)) {
            return true;
        }

        if (!(userDetails instanceof AuthenticatedUser authenticatedUser)) {
            return false;
        }

        return authenticatedUser.tenantId() != null
                && tokenTenantId.equals(authenticatedUser.tenantId().toString());
    }

    private void verifySignature(String signingInput, String encodedSignature) {
        String expectedSignature = sign(signingInput);
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = encodedSignature.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
            throw new JwtValidationException("JWT signature is invalid");
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private byte[] writeValueAsBytes(Map<String, Object> claims) {
        try {
            return objectMapper.writeValueAsBytes(claims);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize JWT claims", exception);
        }
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new JwtValidationException("JWT is malformed");
        }
    }

    private String decodeToString(String encodedValue) {
        try {
            return new String(DECODER.decode(encodedValue), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new JwtValidationException("JWT is malformed");
        }
    }

    private String base64UrlEncode(byte[] value) {
        return ENCODER.encodeToString(value);
    }

    private String requiredText(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || !StringUtils.hasText(fieldNode.asText())) {
            throw new JwtValidationException("JWT is missing required claim: " + fieldName);
        }
        return fieldNode.asText();
    }

    private long requiredLong(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || !fieldNode.canConvertToLong()) {
            throw new JwtValidationException("JWT is missing required claim: " + fieldName);
        }
        return fieldNode.asLong();
    }

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || !StringUtils.hasText(fieldNode.asText())) {
            return null;
        }
        return fieldNode.asText();
    }

    private Optional<UUID> extractTenantId(UserDetails userDetails) {
        if (userDetails instanceof AuthenticatedUser authenticatedUser && authenticatedUser.tenantId() != null) {
            return Optional.of(authenticatedUser.tenantId());
        }
        return Optional.empty();
    }

    private record JwtToken(
            String subject, String issuer, Instant issuedAt, Instant expiresAt, String type, String tenantId) {}

    private static final class JwtValidationException extends RuntimeException {

        private JwtValidationException(String message) {
            super(message);
        }
    }
}
