package com.devrick.pos.exception.branch;

import java.util.UUID;

public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(UUID branchId) {
        super("Branch not found: " + branchId);
    }
}
