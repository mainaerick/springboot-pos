package com.devrick.pos.exception.productcategory;

public class DuplicateProductCategoryCodeException extends RuntimeException {

    public DuplicateProductCategoryCodeException(String code) {
        super("Product category code already exists: " + code);
    }
}
