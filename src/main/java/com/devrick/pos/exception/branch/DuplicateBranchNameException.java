package com.devrick.pos.exception.branch;

public class DuplicateBranchNameException extends RuntimeException {

    public DuplicateBranchNameException(String name) {
        super("Branch name already exists: " + name);
    }
}
