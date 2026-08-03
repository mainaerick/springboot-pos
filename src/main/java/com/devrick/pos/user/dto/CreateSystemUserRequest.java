package com.devrick.pos.user.dto;

import com.devrick.pos.common.enums.Role;

public record CreateSystemUserRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        Role role,
        boolean enabled,
        boolean mustChangePassword) {}
