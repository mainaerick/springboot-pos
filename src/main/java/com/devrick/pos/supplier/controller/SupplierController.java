package com.devrick.pos.supplier.controller;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.exception.ErrorResponse;
import com.devrick.pos.supplier.dto.CreateSupplierRequest;
import com.devrick.pos.supplier.dto.SupplierResponse;
import com.devrick.pos.supplier.dto.UpdateSupplierRequest;
import com.devrick.pos.supplier.dto.UpdateSupplierStatusRequest;
import com.devrick.pos.supplier.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Supplier Management", description = "Create, list, retrieve, update, and activate suppliers.")
@PreAuthorize("isAuthenticated()")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    @Operation(
            summary = "Create supplier",
            description = "Creates a new tenant-owned supplier record. Requires manager or administrator access.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Supplier created",
                content = @Content(schema = @Schema(implementation = SupplierResponse.class))),
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
                description = "Duplicate supplier code",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierResponse response = supplierService.createSupplier(request);
        return ResponseEntity.created(URI.create("/api/v1/suppliers/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List suppliers",
            description =
                    "Returns a paginated tenant-scoped supplier list. Supports search, active filtering, and safe sorting.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Paged supplier list",
                content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<SupplierResponse>> getSuppliers(
            @Parameter(description = "Case-insensitive search by code, name, contact person, email, phone, alternate phone, or tax number")
                    @RequestParam(required = false)
                    String search,
            @Parameter(description = "Optional active filter") @RequestParam(required = false) Boolean active,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(supplierService.getSuppliers(search, active, pageable));
    }

    @GetMapping("/{supplierId}")
    @Operation(summary = "Get supplier", description = "Returns a single supplier owned by the current tenant.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Supplier found",
                content = @Content(schema = @Schema(implementation = SupplierResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Supplier not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SupplierResponse> getSupplier(
            @Parameter(description = "Supplier UUID") @PathVariable UUID supplierId) {
        return ResponseEntity.ok(supplierService.getSupplier(supplierId));
    }

    @PutMapping("/{supplierId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Update supplier", description = "Updates editable supplier profile fields.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Supplier updated",
                content = @Content(schema = @Schema(implementation = SupplierResponse.class))),
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
                description = "Supplier not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate supplier code",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SupplierResponse> updateSupplier(
            @Parameter(description = "Supplier UUID") @PathVariable UUID supplierId,
            @Valid @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplier(supplierId, request));
    }

    @PatchMapping("/{supplierId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Change supplier status", description = "Activates or deactivates a supplier.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Supplier status updated",
                content = @Content(schema = @Schema(implementation = SupplierResponse.class))),
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
                description = "Supplier not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SupplierResponse> updateSupplierStatus(
            @Parameter(description = "Supplier UUID") @PathVariable UUID supplierId,
            @Valid @RequestBody UpdateSupplierStatusRequest request) {
        return ResponseEntity.ok(supplierService.updateSupplierStatus(supplierId, request));
    }
}
