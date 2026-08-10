package com.devrick.pos.supplier.service;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.supplier.dto.CreateSupplierRequest;
import com.devrick.pos.supplier.dto.SupplierResponse;
import com.devrick.pos.supplier.dto.UpdateSupplierRequest;
import com.devrick.pos.supplier.dto.UpdateSupplierStatusRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    SupplierResponse createSupplier(CreateSupplierRequest request);

    PageResponse<SupplierResponse> getSuppliers(String search, Boolean active, Pageable pageable);

    SupplierResponse getSupplier(UUID supplierId);

    SupplierResponse updateSupplier(UUID supplierId, UpdateSupplierRequest request);

    SupplierResponse updateSupplierStatus(UUID supplierId, UpdateSupplierStatusRequest request);
}
