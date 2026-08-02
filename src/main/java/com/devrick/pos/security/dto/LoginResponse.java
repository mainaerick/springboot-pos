package com.devrick.pos.security.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
