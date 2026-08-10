package com.devrick.pos.exception.supplier;

import java.util.UUID;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(UUID supplierId) {
        super("Supplier not found: " + supplierId);
    }
}
