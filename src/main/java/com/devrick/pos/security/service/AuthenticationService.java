package com.devrick.pos.security.service;

import com.devrick.pos.security.dto.LoginRequest;
import com.devrick.pos.security.dto.LoginResponse;
import com.devrick.pos.security.jwt.JwtProperties;
import com.devrick.pos.security.jwt.JwtService;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.mapper.UserMapper;
import com.devrick.pos.user.repository.UserRepository;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserRepository userRepository,
            UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(Locale.ROOT), request.password()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal);
        return new LoginResponse(
                accessToken,
                TOKEN_TYPE,
                jwtProperties.getAccessTokenExpiration().toSeconds());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Principal principal) {
        String normalizedEmail = Optional.ofNullable(principal)
                .map(Principal::getName)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Current authenticated user is missing"));

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));
        return userMapper.toResponse(user);
    }
}
