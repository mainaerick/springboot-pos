package com.devrick.pos.security.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
