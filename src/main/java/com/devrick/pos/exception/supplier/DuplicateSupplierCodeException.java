package com.devrick.pos.exception.supplier;

public class DuplicateSupplierCodeException extends RuntimeException {

    public DuplicateSupplierCodeException(String code) {
        super("Supplier code already exists: " + code);
    }
}
