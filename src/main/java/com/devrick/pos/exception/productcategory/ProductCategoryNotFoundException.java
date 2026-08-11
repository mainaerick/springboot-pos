package com.devrick.pos.exception.productcategory;

import java.util.UUID;

public class ProductCategoryNotFoundException extends RuntimeException {

    public ProductCategoryNotFoundException(UUID categoryId) {
        super("Product category not found: " + categoryId);
    }
}
