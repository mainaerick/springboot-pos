package com.devrick.pos.user.service;

import com.devrick.pos.user.dto.CreateUserRequest;
import com.devrick.pos.user.dto.UpdateUserRequest;
import com.devrick.pos.user.dto.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse getById(UUID id);

    List<UserResponse> getAll();

    UserResponse update(UUID id, UpdateUserRequest request);

    void disable(UUID id);
}
