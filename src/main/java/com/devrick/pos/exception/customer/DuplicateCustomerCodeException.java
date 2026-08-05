package com.devrick.pos.exception.customer;

public class DuplicateCustomerCodeException extends RuntimeException {

    public DuplicateCustomerCodeException(String code) {
        super("Customer code already exists: " + code);
    }
}
