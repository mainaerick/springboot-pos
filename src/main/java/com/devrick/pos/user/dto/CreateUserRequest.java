package com.devrick.pos.user.dto;

import com.devrick.pos.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 2, max = 100) String firstName,
        @NotBlank @Size(min = 2, max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        Role role) {}
