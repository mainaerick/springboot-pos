package com.devrick.pos.security.controller;

import com.devrick.pos.security.dto.LoginRequest;
import com.devrick.pos.security.dto.LoginResponse;
import com.devrick.pos.security.dto.RefreshTokenRequest;
import com.devrick.pos.security.dto.RefreshTokenResponse;
import com.devrick.pos.security.service.AuthenticationService;
import com.devrick.pos.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token refresh, logout, and current-user endpoints.")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticates a user with email and password and returns access and refresh tokens.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Login succeeded",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid login request",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new access token.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Token refreshed",
                content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid refresh token request",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Refresh token is invalid or expired",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Logout",
            description =
                    "Performs the current stateless logout flow. The implementation is a no-op and returns 204 No Content.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logged out"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout() {
        authenticationService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Current user", description = "Returns the authenticated user's public profile fields.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Current user returned",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> me(@Parameter(hidden = true) Principal principal) {
        return ResponseEntity.ok(authenticationService.getCurrentUser(principal));
    }
}
