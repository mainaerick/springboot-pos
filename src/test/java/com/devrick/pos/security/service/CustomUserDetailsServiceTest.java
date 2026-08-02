package com.devrick.pos.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsernameMapsUserToUserDetails() {
        User user = new User();
        user.setEmail("john.doe@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);

        org.mockito.Mockito.when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        UserDetails userDetails = service.loadUserByUsername(" john.doe@example.com ");

        assertEquals("john.doe@example.com", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertFalse(userDetails.getAuthorities().isEmpty());
        assertEquals(
                "ROLE_ADMIN", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        org.mockito.Mockito.when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        UsernameNotFoundException exception =
                assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));

        assertEquals("User not found with email: missing@example.com", exception.getMessage());
    }
}
