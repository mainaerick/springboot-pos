package com.devrick.pos.branch.service;

import com.devrick.pos.branch.dto.BranchResponse;
import com.devrick.pos.branch.dto.CreateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchStatusRequest;
import com.devrick.pos.common.dto.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface BranchService {

    BranchResponse createBranch(CreateBranchRequest request);

    PageResponse<BranchResponse> getBranches(String search, Boolean active, Pageable pageable);

    BranchResponse getBranch(UUID branchId);

    BranchResponse updateBranch(UUID branchId, UpdateBranchRequest request);

    BranchResponse updateBranchStatus(UUID branchId, UpdateBranchStatusRequest request);
}
