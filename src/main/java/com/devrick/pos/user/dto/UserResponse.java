package com.devrick.pos.user.dto;

import com.devrick.pos.common.enums.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {}
