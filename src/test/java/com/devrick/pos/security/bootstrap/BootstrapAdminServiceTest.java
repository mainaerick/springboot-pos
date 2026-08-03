package com.devrick.pos.security.bootstrap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.security.role.entity.AppRole;
import com.devrick.pos.security.role.repository.AppRoleRepository;
import com.devrick.pos.user.dto.CreateSystemUserRequest;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.repository.UserRepository;
import com.devrick.pos.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-03T08:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    void bootstrapDisabledSkipsEverything() {
        BootstrapAdminService service = new BootstrapAdminService(
                new BootstrapAdminProperties(false, "", ""), appRoleRepository, userRepository, userService, clock);

        service.bootstrap();

        verify(appRoleRepository, never()).findByName(any());
        verify(userService, never()).createBootstrapAdmin(any());
    }

    @Test
    void bootstrapCreatesSuperAdminWhenDatabaseEmpty() {
        BootstrapAdminService service = service(true, " Admin@Example.com ", "TemporaryStrongPassword123!");
        AppRole superAdminRole = appRole(Role.SUPER_ADMIN.name());
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UserResponse createdUser = new UserResponse(
                userId,
                "System",
                "Administrator",
                "admin@example.com",
                Role.SUPER_ADMIN,
                true,
                Instant.parse("2026-08-03T08:00:00Z"),
                Instant.parse("2026-08-03T08:00:00Z"));

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name())).thenReturn(Optional.of(superAdminRole));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(userService.createBootstrapAdmin(any())).thenReturn(createdUser);

        service.bootstrap();

        ArgumentCaptor<CreateSystemUserRequest> captor = ArgumentCaptor.forClass(CreateSystemUserRequest.class);
        verify(userService).createBootstrapAdmin(captor.capture());
        assertEquals("admin@example.com", captor.getValue().email());
        assertEquals(Role.SUPER_ADMIN, captor.getValue().role());
        assertEquals(true, captor.getValue().enabled());
        assertEquals(true, captor.getValue().mustChangePassword());
    }

    @Test
    void bootstrapSkipsWhenSuperAdminExists() {
        BootstrapAdminService service = service(true, "admin@example.com", "TemporaryStrongPassword123!");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(true);

        service.bootstrap();

        verify(userService, never()).createBootstrapAdmin(any());
    }

    @Test
    void bootstrapFailsWhenEmailMissing() {
        BootstrapAdminService service = service(true, "   ", "TemporaryStrongPassword123!");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);

        assertThrows(BootstrapAdminConfigurationException.class, service::bootstrap);
        verify(userService, never()).createBootstrapAdmin(any());
    }

    @Test
    void bootstrapFailsWhenPasswordMissing() {
        BootstrapAdminService service = service(true, "admin@example.com", "  ");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);

        assertThrows(BootstrapAdminConfigurationException.class, service::bootstrap);
        verify(userService, never()).createBootstrapAdmin(any());
    }

    @Test
    void bootstrapFailsWhenPasswordIsTooShort() {
        BootstrapAdminService service = service(true, "admin@example.com", "Short1!");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);

        BootstrapAdminConfigurationException exception =
                assertThrows(BootstrapAdminConfigurationException.class, service::bootstrap);

        assertEquals("Bootstrap admin password must be at least 12 characters long", exception.getMessage());
        verify(userService, never()).createBootstrapAdmin(any());
    }

    @Test
    void bootstrapFailsWhenEmailBelongsToNonSuperAdminUser() {
        BootstrapAdminService service = service(true, "admin@example.com", "TemporaryStrongPassword123!");
        User existingUser = new User();
        existingUser.setEmail("admin@example.com");
        existingUser.setRole(Role.ADMIN);

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(existingUser));

        BootstrapAdminConfigurationException exception =
                assertThrows(BootstrapAdminConfigurationException.class, service::bootstrap);

        assertEquals(
                "Bootstrap admin email is already assigned to an existing non-super-admin user",
                exception.getMessage());
        verify(userService, never()).createBootstrapAdmin(any());
    }

    @Test
    void bootstrapNormalizesEmailBeforeStorage() {
        BootstrapAdminService service = service(true, " Admin@Example.com ", "TemporaryStrongPassword123!");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(userService.createBootstrapAdmin(any())).thenReturn(createdResponse());

        service.bootstrap();

        ArgumentCaptor<CreateSystemUserRequest> captor = ArgumentCaptor.forClass(CreateSystemUserRequest.class);
        verify(userService).createBootstrapAdmin(captor.capture());
        assertEquals("admin@example.com", captor.getValue().email());
    }

    @Test
    void bootstrapIsIdempotentAcrossRepeatedCalls() {
        BootstrapAdminService service = service(true, "admin@example.com", "TemporaryStrongPassword123!");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false, true);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty(), Optional.empty());
        when(userService.createBootstrapAdmin(any())).thenReturn(createdResponse());

        service.bootstrap();
        service.bootstrap();

        verify(userService).createBootstrapAdmin(any());
    }

    @Test
    void bootstrapHandlesDuplicateCreationSafely() {
        BootstrapAdminService service = service(true, "admin@example.com", "TemporaryStrongPassword123!");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false, true);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(userService.createBootstrapAdmin(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDoesNotThrow(service::bootstrap);
    }

    @Test
    void bootstrapSetsMustChangePasswordFlagWhenCreatingAdmin() {
        BootstrapAdminService service = service(true, "admin@example.com", "TemporaryStrongPassword123!");

        when(appRoleRepository.findByName(Role.SUPER_ADMIN.name()))
                .thenReturn(Optional.of(appRole(Role.SUPER_ADMIN.name())));
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(userService.createBootstrapAdmin(any())).thenReturn(createdResponse());

        service.bootstrap();

        ArgumentCaptor<CreateSystemUserRequest> captor = ArgumentCaptor.forClass(CreateSystemUserRequest.class);
        verify(userService).createBootstrapAdmin(captor.capture());
        assertEquals(true, captor.getValue().mustChangePassword());
    }

    private BootstrapAdminService service(boolean enabled, String email, String password) {
        return new BootstrapAdminService(
                new BootstrapAdminProperties(enabled, email, password),
                appRoleRepository,
                userRepository,
                userService,
                clock);
    }

    private AppRole appRole(String name) {
        AppRole appRole = new AppRole();
        appRole.setName(name);
        return appRole;
    }

    private UserResponse createdResponse() {
        return new UserResponse(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "System",
                "Administrator",
                "admin@example.com",
                Role.SUPER_ADMIN,
                true,
                Instant.parse("2026-08-03T08:00:00Z"),
                Instant.parse("2026-08-03T08:00:00Z"));
    }
}
