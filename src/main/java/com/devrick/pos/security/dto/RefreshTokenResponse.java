package com.devrick.pos.security.dto;

public record RefreshTokenResponse(String accessToken, long expiresIn) {}
