package com.crowdfund.service;

import com.crowdfund.entity.Role;
import com.crowdfund.entity.User;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "hashedPassword", Role.CONTRIBUTOR);
        testUser.setId(1L);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register a new user with hashed password")
        void shouldRegisterNewUser() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });

            User result = userService.register("New User", "new@example.com",
                    "rawPassword", Role.CREATOR);

            assertNotNull(result);
            assertEquals("New User", result.getName());
            assertEquals("new@example.com", result.getEmail());
            assertEquals("$2a$10$hashed", result.getPasswordHash());
            assertEquals(Role.CREATOR, result.getRole());

            verify(passwordEncoder).encode("rawPassword");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw when email already exists")
        void shouldThrowOnDuplicateEmail() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                    userService.register("Test", "test@example.com", "pass", Role.CONTRIBUTOR));

            assertTrue(ex.getMessage().contains("already registered"));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("should update user profile without changing password when blank")
        void shouldUpdateWithoutPasswordChange() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateUser(1L, "Updated Name",
                    "test@example.com", "", null, testUser);

            assertEquals("Updated Name", result.getName());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("should re-hash password when a new one is provided")
        void shouldRehashPassword() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$newHash");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, "Test User", "test@example.com",
                    "newPass", null, testUser);

            verify(passwordEncoder).encode("newPass");
            assertEquals("$2a$10$newHash", testUser.getPasswordHash());
        }

        @Test
        @DisplayName("should throw when changing to an email that already exists")
        void shouldThrowOnDuplicateEmailChange() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThrows(BusinessRuleException.class, () ->
                    userService.updateUser(1L, "Test", "taken@example.com",
                            null, null, testUser));
        }
    }

    @Nested
    @DisplayName("deactivateUser()")
    class DeactivateUser {

        @Test
        @DisplayName("should soft-delete by setting active to false")
        void shouldDeactivate() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            assertTrue(testUser.isActive());

            userService.deactivateUser(1L, testUser);

            assertFalse(testUser.isActive());
            verify(userRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("admin should be able to create users with any role")
        void adminCreatesUser() {
            User admin = new User("Admin", "admin@test.com", "hash", Role.ADMIN);
            admin.setId(99L);

            when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("pass")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(3L);
                return u;
            });

            User result = userService.createUser("New", "new@test.com",
                    "pass", Role.ADMIN, admin);

            assertEquals(Role.ADMIN, result.getRole());
            verify(userRepository).save(any(User.class));
        }
    }
}
