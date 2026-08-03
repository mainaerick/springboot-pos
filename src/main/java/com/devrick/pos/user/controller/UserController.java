package com.devrick.pos.user.controller;

import com.devrick.pos.user.dto.CreateUserRequest;
import com.devrick.pos.user.dto.UpdateUserRequest;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "Create, read, update, and disable application users.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user account. Requires SUPER_ADMIN or ADMIN.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "User created",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Email already exists",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by id",
            description = "Returns a single user record by UUID. Requires SUPER_ADMIN or ADMIN.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "User found",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getById(@Parameter(description = "User UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    @Operation(
            summary = "List users",
            description =
                    "Returns a paginated list of users. Page is zero-based, size is the number of records per page, and sort accepts property,direction values such as firstName,asc. Requires SUPER_ADMIN or ADMIN.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Paged user list",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<Page<UserResponse>> getAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(pageable));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update user",
            description = "Updates an existing user profile. Requires SUPER_ADMIN or ADMIN.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "User updated",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Email already exists",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable user", description = "Marks the user as disabled. Requires SUPER_ADMIN or ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User disabled"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(schema = @Schema(implementation = com.devrick.pos.exception.ErrorResponse.class)))
    })
    public ResponseEntity<Void> disable(@Parameter(description = "User UUID") @PathVariable UUID id) {
        userService.disable(id);
        return ResponseEntity.noContent().build();
    }
}
