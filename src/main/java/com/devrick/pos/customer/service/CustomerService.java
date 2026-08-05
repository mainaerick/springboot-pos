package com.devrick.pos.customer.service;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.customer.dto.CreateCustomerRequest;
import com.devrick.pos.customer.dto.CustomerResponse;
import com.devrick.pos.customer.dto.UpdateCustomerRequest;
import com.devrick.pos.customer.dto.UpdateCustomerStatusRequest;
import com.devrick.pos.customer.entity.CustomerType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest request);

    PageResponse<CustomerResponse> getCustomers(String search, CustomerType type, Boolean active, Pageable pageable);

    CustomerResponse getCustomer(UUID customerId);

    CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest request);

    CustomerResponse updateCustomerStatus(UUID customerId, UpdateCustomerStatusRequest request);
}
