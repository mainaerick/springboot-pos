package com.devrick.pos.security.service;

import com.devrick.pos.security.dto.LoginRequest;
import com.devrick.pos.security.dto.LoginResponse;
import com.devrick.pos.security.dto.RefreshTokenRequest;
import com.devrick.pos.security.dto.RefreshTokenResponse;
import com.devrick.pos.security.jwt.JwtProperties;
import com.devrick.pos.security.jwt.JwtService;
import com.devrick.pos.security.principal.AuthenticatedUser;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.mapper.UserMapper;
import com.devrick.pos.user.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final UserDetailsService userDetailsService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UserRepository userRepository,
            UserMapper userMapper,
            UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userDetailsService = userDetailsService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(Locale.ROOT), request.password()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String normalizedEmail = principal.getUsername().trim().toLowerCase(Locale.ROOT);
        boolean mustChangePassword = principal instanceof AuthenticatedUser authenticatedUser
                ? authenticatedUser.mustChangePassword()
                : userRepository
                        .findByEmailIgnoreCase(normalizedEmail)
                        .map(User::isMustChangePassword)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        return new LoginResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtProperties.getAccessTokenExpiration().toSeconds(),
                mustChangePassword);
    }

    @Transactional(readOnly = true)
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken().trim();
        try {
            String username = jwtService.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
                throw new BadCredentialsException("Refresh token is invalid");
            }

            String accessToken = jwtService.generateAccessToken(userDetails);
            return new RefreshTokenResponse(
                    accessToken, jwtProperties.getAccessTokenExpiration().toSeconds());
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Refresh token is invalid", exception);
        }
    }

    public void logout() {
        // Stateless JWT logout is a no-op in Story 7.
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(java.security.Principal principal) {
        User user = Optional.ofNullable(principal)
                .map(value -> value instanceof Authentication authentication ? authentication : null)
                .map(Authentication::getPrincipal)
                .filter(AuthenticatedUser.class::isInstance)
                .map(AuthenticatedUser.class::cast)
                .flatMap(authenticatedUser ->
                        userRepository.findByIdAndTenantId(authenticatedUser.userId(), authenticatedUser.tenantId()))
                .orElseGet(() -> {
                    String normalizedEmail = Optional.ofNullable(principal)
                            .map(java.security.Principal::getName)
                            .map(value -> value.trim().toLowerCase(Locale.ROOT))
                            .orElseThrow(() -> new UsernameNotFoundException("Current authenticated user is missing"));
                    return userRepository
                            .findByEmailIgnoreCase(normalizedEmail)
                            .orElseThrow(() ->
                                    new UsernameNotFoundException("User not found with email: " + normalizedEmail));
                });
        return userMapper.toResponse(user);
    }
}
