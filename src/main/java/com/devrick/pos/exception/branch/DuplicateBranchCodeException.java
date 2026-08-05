package com.devrick.pos.exception.branch;

public class DuplicateBranchCodeException extends RuntimeException {

    public DuplicateBranchCodeException(String code) {
        super("Branch code already exists: " + code);
    }
}
