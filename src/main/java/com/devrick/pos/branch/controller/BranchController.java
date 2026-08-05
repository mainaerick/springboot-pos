package com.devrick.pos.branch.controller;

import com.devrick.pos.branch.dto.BranchResponse;
import com.devrick.pos.branch.dto.CreateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchStatusRequest;
import com.devrick.pos.branch.service.BranchService;
import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branches")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Branch Management", description = "Create, list, retrieve, update, and activate branches.")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create branch", description = "Creates a new tenant-owned branch. Requires administrator access.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Branch created",
                content = @Content(schema = @Schema(implementation = BranchResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate branch code or name",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody CreateBranchRequest request) {
        BranchResponse response = branchService.createBranch(request);
        return ResponseEntity.created(URI.create("/api/v1/branches/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "List branches",
            description =
                    "Returns a paginated tenant-scoped branch list. Supports optional search by name or code, optional active filtering, and safe sorting.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Paged branch list",
                content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<BranchResponse>> getBranches(
            @Parameter(description = "Case-insensitive search by branch name or code") @RequestParam(required = false)
                    String search,
            @Parameter(description = "Optional active filter") @RequestParam(required = false) Boolean active,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(branchService.getBranches(search, active, pageable));
    }

    @GetMapping("/{branchId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get branch", description = "Returns a single branch owned by the current tenant.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Branch found",
                content = @Content(schema = @Schema(implementation = BranchResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Branch not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BranchResponse> getBranch(
            @Parameter(description = "Branch UUID") @PathVariable UUID branchId) {
        return ResponseEntity.ok(branchService.getBranch(branchId));
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update branch", description = "Updates editable branch profile fields. Requires administrator access.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Branch updated",
                content = @Content(schema = @Schema(implementation = BranchResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Branch not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate branch name",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BranchResponse> update(
            @Parameter(description = "Branch UUID") @PathVariable UUID branchId,
            @Valid @RequestBody UpdateBranchRequest request) {
        return ResponseEntity.ok(branchService.updateBranch(branchId, request));
    }

    @PatchMapping("/{branchId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
            summary = "Change branch status",
            description = "Activates or deactivates a branch. Requires administrator access.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Branch status updated",
                content = @Content(schema = @Schema(implementation = BranchResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Not enough privileges",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Branch not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BranchResponse> updateStatus(
            @Parameter(description = "Branch UUID") @PathVariable UUID branchId,
            @Valid @RequestBody UpdateBranchStatusRequest request) {
        return ResponseEntity.ok(branchService.updateBranchStatus(branchId, request));
    }
}
