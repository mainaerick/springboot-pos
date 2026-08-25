package com.devrick.pos.exception.productcategory;

public class DuplicateProductCategoryNameException extends RuntimeException {

    public DuplicateProductCategoryNameException(String name) {
        super("Product category name already exists: " + name);
    }
}
