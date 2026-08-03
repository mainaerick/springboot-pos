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
import com.devrick.pos.security.principal.AuthenticatedUser;
import com.devrick.pos.tenant.entity.TenantStatus;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.mapper.UserMapper;
import com.devrick.pos.user.repository.UserRepository;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

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
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("pos-api");
        jwtProperties.setSecret("pos-test-secret-key-pos-test-secret-key");
        jwtProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExpiration(Duration.ofDays(7));
        authenticationService = new AuthenticationService(
                authenticationManager, jwtService, jwtProperties, userRepository, userMapper, userDetailsService);
        authenticatedUser = principal(false);
    }

    @Test
    void loginReturnsAccessToken() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                authenticatedUser, null, authenticatedUser.getAuthorities());
        User user = buildUser(false);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateAccessToken(authenticatedUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(authenticatedUser)).thenReturn("refresh-token");
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
        User user = buildUser(false);
        UserResponse expected = new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                authenticatedUser, null, authenticatedUser.getAuthorities());

        when(userRepository.findByIdAndTenantId(user.getId(), TENANT_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        Principal principal = authentication;
        assertEquals(expected, authenticationService.getCurrentUser(principal));
    }

    @Test
    void refreshReturnsNewAccessToken() {
        when(jwtService.extractUsername("refresh-token")).thenReturn("john.doe@example.com");
        when(userDetailsService.loadUserByUsername("john.doe@example.com")).thenReturn(authenticatedUser);
        when(jwtService.isRefreshTokenValid("refresh-token", authenticatedUser)).thenReturn(true);
        when(jwtService.generateAccessToken(authenticatedUser)).thenReturn("new-access-token");

        RefreshTokenResponse response = authenticationService.refresh(new RefreshTokenRequest("refresh-token"));

        assertEquals("new-access-token", response.accessToken());
        assertEquals(900, response.expiresIn());
    }

    @Test
    void refreshRejectsInvalidRefreshToken() {
        when(jwtService.extractUsername("refresh-token")).thenReturn("john.doe@example.com");
        when(userDetailsService.loadUserByUsername("john.doe@example.com")).thenReturn(authenticatedUser);
        when(jwtService.isRefreshTokenValid("refresh-token", authenticatedUser)).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.refresh(new RefreshTokenRequest("refresh-token")));
    }

    private AuthenticatedUser principal(boolean mustChangePassword) {
        return new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                TENANT_ID,
                "Default Business",
                "DEFAULT",
                TenantStatus.ACTIVE,
                "john.doe@example.com",
                "encoded-password",
                true,
                mustChangePassword,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private User buildUser(boolean mustChangePassword) {
        User user = new User();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        user.setMustChangePassword(mustChangePassword);
        user.setCreatedAt(Instant.parse("2026-08-02T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-08-02T10:05:00Z"));
        return user;
    }
}
