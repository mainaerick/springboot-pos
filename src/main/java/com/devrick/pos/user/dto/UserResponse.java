package com.devrick.pos.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {}
