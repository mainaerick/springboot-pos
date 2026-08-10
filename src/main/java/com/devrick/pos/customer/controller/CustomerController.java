package com.devrick.pos.customer.controller;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.customer.dto.CreateCustomerRequest;
import com.devrick.pos.customer.dto.CustomerResponse;
import com.devrick.pos.customer.dto.UpdateCustomerRequest;
import com.devrick.pos.customer.dto.UpdateCustomerStatusRequest;
import com.devrick.pos.customer.entity.CustomerType;
import com.devrick.pos.customer.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer Management", description = "Create, list, retrieve, update, and activate customers.")
@PreAuthorize("isAuthenticated()")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(
            summary = "Create customer",
            description =
                    "Creates a new tenant-owned customer record. Requires sales, manager, or administrator access.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Customer created",
                content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
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
                description = "Duplicate customer code",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List customers",
            description =
                    "Returns a paginated tenant-scoped customer list. Supports search, customer-type filtering, active filtering, and safe sorting.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Paged customer list",
                content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<CustomerResponse>> getCustomers(
            @Parameter(description = "Case-insensitive search by code, name, email, phone, or tax number")
                    @RequestParam(required = false)
                    String search,
            @Parameter(description = "Optional customer type filter") @RequestParam(required = false) CustomerType type,
            @Parameter(description = "Optional active filter") @RequestParam(required = false) Boolean active,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomers(search, type, active, pageable));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer", description = "Returns a single customer owned by the current tenant.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Customer found",
                content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Customer not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer UUID") @PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.getCustomer(customerId));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Update customer", description = "Updates editable customer profile fields.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Customer updated",
                content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
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
                description = "Customer not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate customer code",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer UUID") @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    @PatchMapping("/{customerId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Change customer status", description = "Activates or deactivates a customer.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Customer status updated",
                content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
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
                description = "Customer not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> updateCustomerStatus(
            @Parameter(description = "Customer UUID") @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerStatusRequest request) {
        return ResponseEntity.ok(customerService.updateCustomerStatus(customerId, request));
    }
}
