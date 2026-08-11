package com.devrick.pos.productcategory.controller;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.exception.ErrorResponse;
import com.devrick.pos.productcategory.dto.CreateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.ProductCategoryResponse;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryStatusRequest;
import com.devrick.pos.productcategory.service.ProductCategoryService;
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
@RequestMapping("/api/v1/product-categories")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Product Category Management", description = "Create, list, retrieve, update, and activate product categories.")
@PreAuthorize("isAuthenticated()")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    public ProductCategoryController(ProductCategoryService productCategoryService) {
        this.productCategoryService = productCategoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    @Operation(
            summary = "Create product category",
            description = "Creates a new tenant-owned product category. Requires manager or administrator access.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Category created",
                content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class))),
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
                description = "Duplicate category code or name",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductCategoryResponse> create(@Valid @RequestBody CreateProductCategoryRequest request) {
        ProductCategoryResponse response = productCategoryService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/v1/product-categories/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List product categories",
            description =
                    "Returns a paginated tenant-scoped category list. Supports case-insensitive search by code, name, or description, optional active filtering, and safe sorting.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Paged category list",
                content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<ProductCategoryResponse>> getCategories(
            @Parameter(description = "Case-insensitive search by code, name, or description") @RequestParam(required = false)
                    String search,
            @Parameter(description = "Optional active filter") @RequestParam(required = false) Boolean active,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(productCategoryService.getCategories(search, active, pageable));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get product category", description = "Returns a single category owned by the current tenant.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Category found",
                content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Category not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductCategoryResponse> getCategory(
            @Parameter(description = "Category UUID") @PathVariable UUID categoryId) {
        return ResponseEntity.ok(productCategoryService.getCategory(categoryId));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Update product category", description = "Updates editable category fields.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Category updated",
                content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class))),
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
                description = "Category not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate category name",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductCategoryResponse> updateCategory(
            @Parameter(description = "Category UUID") @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateProductCategoryRequest request) {
        return ResponseEntity.ok(productCategoryService.updateCategory(categoryId, request));
    }

    @PatchMapping("/{categoryId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Change product category status", description = "Activates or deactivates a product category.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Category status updated",
                content = @Content(schema = @Schema(implementation = ProductCategoryResponse.class))),
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
                description = "Category not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductCategoryResponse> updateCategoryStatus(
            @Parameter(description = "Category UUID") @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateProductCategoryStatusRequest request) {
        return ResponseEntity.ok(productCategoryService.updateCategoryStatus(categoryId, request));
    }
}
