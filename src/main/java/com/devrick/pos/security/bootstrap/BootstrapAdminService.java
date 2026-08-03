package com.devrick.pos.security.bootstrap;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.security.role.repository.AppRoleRepository;
import com.devrick.pos.user.dto.CreateSystemUserRequest;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.repository.UserRepository;
import com.devrick.pos.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BootstrapAdminService {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminService.class);
    private static final String ROLE_SEED_NAME = Role.SUPER_ADMIN.name();
    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final BootstrapAdminProperties properties;
    private final AppRoleRepository appRoleRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final Clock clock;

    public BootstrapAdminService(
            BootstrapAdminProperties properties,
            AppRoleRepository appRoleRepository,
            UserRepository userRepository,
            UserService userService,
            Clock clock) {
        this.properties = properties;
        this.appRoleRepository = appRoleRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public void bootstrap() {
        if (!properties.enabled()) {
            return;
        }

        appRoleRepository
                .findByName(ROLE_SEED_NAME)
                .orElseThrow(() -> new BootstrapAdminConfigurationException(
                        "Bootstrap admin cannot start because the SUPER_ADMIN role seed is missing"));

        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            return;
        }

        String normalizedEmail = normalizeEmail(properties.email());
        validateEmail(normalizedEmail);
        validatePassword(properties.password());

        User existingUser =
                userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() == Role.SUPER_ADMIN) {
                return;
            }

            throw new BootstrapAdminConfigurationException(
                    "Bootstrap admin email is already assigned to an existing non-super-admin user");
        }

        CreateSystemUserRequest request = new CreateSystemUserRequest(
                "System", "Administrator", normalizedEmail, properties.password(), Role.SUPER_ADMIN, true, true);

        try {
            UserResponse createdUser = userService.createBootstrapAdmin(request);
            if (createdUser == null) {
                throw new BootstrapAdminConfigurationException("Bootstrap admin creation did not persist the user");
            }

            log.info(
                    "BOOTSTRAP_ADMIN_CREATED userId={} email={} timestamp={} source=SYSTEM_BOOTSTRAP",
                    createdUser.id(),
                    normalizedEmail,
                    Instant.now(clock));
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
                return;
            }
            throw new BootstrapAdminConfigurationException(
                    "Bootstrap admin could not be created because the database rejected the insert");
        } catch (RuntimeException exception) {
            if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
                return;
            }
            if (userRepository
                    .findByEmailIgnoreCase(normalizedEmail)
                    .map(User::getRole)
                    .filter(Role.SUPER_ADMIN::equals)
                    .isPresent()) {
                return;
            }
            throw exception;
        }
    }

    private String normalizeEmail(String email) {
        return Objects.requireNonNullElse(email, "").trim().toLowerCase(Locale.ROOT);
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BootstrapAdminConfigurationException(
                    "Bootstrap admin is enabled but BOOTSTRAP_ADMIN_EMAIL is missing");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BootstrapAdminConfigurationException(
                    "Bootstrap admin is enabled but BOOTSTRAP_ADMIN_EMAIL is invalid");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BootstrapAdminConfigurationException(
                    "Bootstrap admin is enabled but BOOTSTRAP_ADMIN_PASSWORD is missing");
        }

        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new BootstrapAdminConfigurationException(
                    "Bootstrap admin password must be at least " + MINIMUM_PASSWORD_LENGTH + " characters long");
        }
    }
}
