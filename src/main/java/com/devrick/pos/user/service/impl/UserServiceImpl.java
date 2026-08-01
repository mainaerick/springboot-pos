package com.devrick.pos.user.service.impl;

import com.devrick.pos.exception.user.DuplicateEmailException;
import com.devrick.pos.exception.user.UserNotFoundException;
import com.devrick.pos.user.dto.CreateUserRequest;
import com.devrick.pos.user.dto.UpdateUserRequest;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.mapper.UserMapper;
import com.devrick.pos.user.repository.UserRepository;
import com.devrick.pos.user.service.UserService;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.info("Creating user with email {}", normalizedEmail);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = userMapper.toEntity(request);
        user.setEmail(normalizedEmail);

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findUserById(id);
        String normalizedEmail = normalizeEmail(request.email());
        String currentEmail = normalizeEmail(user.getEmail());
        log.info("Updating user {}", id);

        if (!normalizedEmail.equals(currentEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        userMapper.updateEntity(request, user);
        user.setEmail(normalizedEmail);

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public void disable(UUID id) {
        User user = findUserById(id);
        user.setEnabled(false);
        log.info("Disabling user {}", id);
        userRepository.save(user);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
