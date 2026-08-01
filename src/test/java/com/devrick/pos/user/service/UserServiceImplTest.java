package com.devrick.pos.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devrick.pos.exception.user.DuplicateEmailException;
import com.devrick.pos.exception.user.UserNotFoundException;
import com.devrick.pos.user.dto.CreateUserRequest;
import com.devrick.pos.user.dto.UpdateUserRequest;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.mapper.UserMapper;
import com.devrick.pos.user.repository.UserRepository;
import com.devrick.pos.user.service.impl.UserServiceImpl;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper);
    }

    @Test
    void createSucceedsAndNormalizesEmail() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-31T10:15:30Z");
        CreateUserRequest request = new CreateUserRequest("John", "Doe", " John.Doe@Example.com ", "Password123!");

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setField(user, "id", id);
            setField(user, "createdAt", now);
            setField(user, "updatedAt", now);
            return user;
        });

        UserResponse response = userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userRepository).save(captor.capture());
        assertEquals("John", captor.getValue().getFirstName());
        assertEquals("Doe", captor.getValue().getLastName());
        assertEquals("john.doe@example.com", captor.getValue().getEmail());
        assertEquals("Password123!", captor.getValue().getPassword());
        assertNotNull(response.id());
        assertEquals(id, response.id());
        assertEquals("john.doe@example.com", response.email());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void createDuplicateEmailThrowsDuplicateEmailException() {
        CreateUserRequest request = new CreateUserRequest("John", "Doe", "john.doe@example.com", "Password123!");

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        DuplicateEmailException exception =
                assertThrows(DuplicateEmailException.class, () -> userService.create(request));

        assertEquals("Email already exists: john.doe@example.com", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsExistingUser() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-31T10:15:30Z");
        User user = buildUser(id, "Jane", "Doe", "jane.doe@example.com", "Password123!", true, now, now);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.getById(id);

        assertEquals(id, response.id());
        assertEquals("Jane", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("jane.doe@example.com", response.email());
        assertEquals(true, response.enabled());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.getById(id));

        assertEquals("User not found: " + id, exception.getMessage());
    }

    @Test
    void getAllReturnsDtoList() {
        Instant now = Instant.parse("2026-07-31T10:15:30Z");
        User first =
                buildUser(UUID.randomUUID(), "Jane", "Doe", "jane.doe@example.com", "Password123!", true, now, now);
        User second = buildUser(
                UUID.randomUUID(), "John", "Smith", "john.smith@example.com", "Password123!", false, now, now);

        when(userRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(first, second)));

        Page<UserResponse> responses = userService.getAll(PageRequest.of(0, 20));

        assertEquals(2, responses.getTotalElements());
        assertEquals("jane.doe@example.com", responses.getContent().get(0).email());
        assertEquals("john.smith@example.com", responses.getContent().get(1).email());
    }

    @Test
    void updateSucceedsAndPreservesId() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-31T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-07-31T11:15:30Z");
        User user = buildUser(id, "Jane", "Doe", "jane.doe@example.com", "Password123!", true, createdAt, createdAt);
        UpdateUserRequest request = new UpdateUserRequest("Janet", "Roe", " janet.roe@example.com ", false);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("janet.roe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            setField(saved, "updatedAt", updatedAt);
            return saved;
        });

        UserResponse response = userService.update(id, request);

        verify(userRepository).existsByEmail("janet.roe@example.com");
        assertEquals(id, response.id());
        assertEquals("Janet", response.firstName());
        assertEquals("Roe", response.lastName());
        assertEquals("janet.roe@example.com", response.email());
        assertFalse(response.enabled());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void updateRejectsDuplicateEmailWhenEmailChanges() {
        UUID id = UUID.randomUUID();
        User user = buildUser(
                id, "Jane", "Doe", "jane.doe@example.com", "Password123!", true, Instant.now(), Instant.now());
        UpdateUserRequest request = new UpdateUserRequest("Janet", "Roe", "janet.roe@example.com", true);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("janet.roe@example.com")).thenReturn(true);

        DuplicateEmailException exception =
                assertThrows(DuplicateEmailException.class, () -> userService.update(id, request));

        assertEquals("Email already exists: janet.roe@example.com", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void disableSetsEnabledFalse() {
        UUID id = UUID.randomUUID();
        User user = buildUser(
                id, "Jane", "Doe", "jane.doe@example.com", "Password123!", true, Instant.now(), Instant.now());

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.disable(id);

        verify(userRepository).save(user);
        assertFalse(user.isEnabled());
    }

    @Test
    void mapperMapsCreateRequestAndResponseFields() {
        Instant now = Instant.parse("2026-07-31T10:15:30Z");
        CreateUserRequest request = new CreateUserRequest("Jane", "Doe", "jane.doe@example.com", "Password123!");
        User user = buildUser(UUID.randomUUID(), "Jane", "Doe", "jane.doe@example.com", "Password123!", true, now, now);

        User mappedUser = userMapper.toEntity(request);
        User mappedUserForUpdate = userMapper.toEntity(request);
        UserResponse response = userMapper.toResponse(user);
        UpdateUserRequest updateRequest = new UpdateUserRequest("Janet", "Roe", "janet.roe@example.com", false);
        userMapper.updateEntity(updateRequest, mappedUserForUpdate);

        assertEquals("Jane", mappedUser.getFirstName());
        assertEquals("Jane", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("jane.doe@example.com", response.email());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
        assertEquals("Janet", mappedUserForUpdate.getFirstName());
        assertEquals("Roe", mappedUserForUpdate.getLastName());
        assertEquals("janet.roe@example.com", mappedUserForUpdate.getEmail());
        assertFalse(mappedUserForUpdate.isEnabled());
    }

    private static User buildUser(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String password,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        User user = newUserInstance();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);
        user.setEnabled(enabled);
        setField(user, "id", id);
        setField(user, "createdAt", createdAt);
        setField(user, "updatedAt", updatedAt);
        return user;
    }

    private static User newUserInstance() {
        try {
            java.lang.reflect.Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to instantiate User", exception);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> currentType = target.getClass();

        while (currentType != null) {
            try {
                Field field = currentType.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentType = currentType.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to set field: " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
