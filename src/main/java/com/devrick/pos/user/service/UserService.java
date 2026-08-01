package com.devrick.pos.user.service;

import com.devrick.pos.user.dto.CreateUserRequest;
import com.devrick.pos.user.dto.UpdateUserRequest;
import com.devrick.pos.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse getById(UUID id);

    Page<UserResponse> getAll(Pageable pageable);

    UserResponse update(UUID id, UpdateUserRequest request);

    void disable(UUID id);
}
