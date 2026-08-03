package com.devrick.pos.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.security.dto.LoginRequest;
import com.devrick.pos.security.dto.LoginResponse;
import com.devrick.pos.security.dto.RefreshTokenRequest;
import com.devrick.pos.security.dto.RefreshTokenResponse;
import com.devrick.pos.security.jwt.JwtProperties;
import com.devrick.pos.security.jwt.JwtService;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.mapper.UserMapper;
import com.devrick.pos.user.repository.UserRepository;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserDetailsService userDetailsService;

    private JwtProperties jwtProperties;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("pos-api");
        jwtProperties.setSecret("pos-test-secret-key-pos-test-secret-key");
        jwtProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExpiration(Duration.ofDays(7));
        authenticationService = new AuthenticationService(
                authenticationManager, jwtService, jwtProperties, userRepository, userMapper, userDetailsService);
    }

    @Test
    void loginReturnsAccessToken() {
        var userDetails = new org.springframework.security.core.userdetails.User(
                "john.doe@example.com",
                "encoded-password",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
        User user = new User();
        user.setEmail("john.doe@example.com");
        user.setMustChangePassword(false);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));

        LoginResponse response = authenticationService.login(new LoginRequest(" JOHN.DOE@EXAMPLE.COM ", "Password123"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("john.doe@example.com", captor.getValue().getName());
        assertEquals("Password123", captor.getValue().getCredentials());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals(false, response.mustChangePassword());
    }

    @Test
    void loginPropagatesAuthenticationFailure() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(new LoginRequest("john@example.com", "wrong")));
    }

    @Test
    void getCurrentUserReturnsUserResponse() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        user.setMustChangePassword(false);
        user.setCreatedAt(Instant.parse("2026-08-02T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-08-02T10:05:00Z"));

        UserResponse expected = new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());

        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        Principal principal = () -> "john.doe@example.com";
        assertEquals(expected, authenticationService.getCurrentUser(principal));
    }

    @Test
    void refreshReturnsNewAccessToken() {
        var userDetails = new org.springframework.security.core.userdetails.User(
                "john.doe@example.com",
                "encoded-password",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(jwtService.extractUsername("refresh-token")).thenReturn("john.doe@example.com");
        when(userDetailsService.loadUserByUsername("john.doe@example.com")).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid("refresh-token", userDetails)).thenReturn(true);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("new-access-token");

        RefreshTokenResponse response = authenticationService.refresh(new RefreshTokenRequest("refresh-token"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals(900, response.expiresIn());
    }

    @Test
    void refreshRejectsInvalidRefreshToken() {
        var userDetails = new org.springframework.security.core.userdetails.User(
                "john.doe@example.com",
                "encoded-password",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(jwtService.extractUsername("refresh-token")).thenReturn("john.doe@example.com");
        when(userDetailsService.loadUserByUsername("john.doe@example.com")).thenReturn(userDetails);
        when(jwtService.isRefreshTokenValid("refresh-token", userDetails)).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.refresh(new RefreshTokenRequest("refresh-token")));
    }
}
