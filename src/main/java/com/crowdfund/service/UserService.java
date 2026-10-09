package com.crowdfund.service;

import com.crowdfund.entity.Role;
import com.crowdfund.entity.User;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.exception.ResourceNotFoundException;
import com.crowdfund.repository.UserRepository;
import com.crowdfund.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages user lifecycle: registration, profile updates, role changes,
 * and soft deletion.
 * <p>
 * Passwords are always hashed with BCrypt before persisting. Email
 * uniqueness is enforced at both the service and database level.
 * </p>
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    /**
     * Registers a new user. The password is hashed before storage.
     *
     * @throws BusinessRuleException if the email is already registered
     */
    public User register(String name, String email, String rawPassword, Role role) {
        String cleanEmail = InputSanitizer.validateEmail(email);
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessRuleException("Email already registered: " + cleanEmail);
        }
        if (role == Role.ADMIN) {
            throw new BusinessRuleException("Self-registration as ADMIN is not permitted");
        }
        String cleanName = InputSanitizer.validateAndSanitize("Name", name, 2, 100);
        InputSanitizer.validatePasswordStrength(rawPassword);
        User user = new User(cleanName, cleanEmail, passwordEncoder.encode(rawPassword), role);
        user = userRepository.save(user);
        activityLogService.log("USER_REGISTERED",
                "New " + role + " registered: " + cleanEmail, user);
        log.info("Registered user: {} with role {}", cleanEmail, role);
        return user;
    }

    /**
     * Admin-only: creates a user with any role, including ADMIN.
     */
    public User createUser(String name, String email, String rawPassword, Role role, User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only administrators can create users");
        }
        String cleanName = InputSanitizer.validateAndSanitize("Name", name, 2, 100);
        String cleanEmail = InputSanitizer.validateEmail(email);
        InputSanitizer.validatePasswordStrength(rawPassword);

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessRuleException("Email already registered: " + cleanEmail);
        }
        User user = new User(cleanName, cleanEmail, passwordEncoder.encode(rawPassword), role);
        user = userRepository.save(user);
        activityLogService.log("USER_CREATED",
                "Admin created user: " + cleanEmail + " (" + role + ")", admin);
        log.info("Admin {} created user: {} ({})", admin.getEmail(), cleanEmail, role);
        return user;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> search(String term, Pageable pageable) {
        if (term == null || term.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.searchByNameOrEmail(term, pageable);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Updates a user's profile. If the email changes, uniqueness is rechecked.
     * If a new raw password is provided (non-blank), it is re-hashed.
     */
    public User updateUser(Long userId, String name, String email,
                           String rawPassword, Role role, User actor) {
        User user = findById(userId);

        boolean isSelf = actor != null && user.getId().equals(actor.getId());
        boolean isAdmin = actor != null && actor.getRole() == Role.ADMIN;
        if (!isSelf && !isAdmin) {
            throw new BusinessRuleException("You do not have permission to update this user profile");
        }

        String cleanName = InputSanitizer.validateAndSanitize("Name", name, 2, 100);
        String cleanEmail = InputSanitizer.validateEmail(email);

        if (!user.getEmail().equalsIgnoreCase(cleanEmail) && userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessRuleException("Email already in use: " + cleanEmail);
        }

        user.setName(cleanName);
        user.setEmail(cleanEmail);
        if (rawPassword != null && !rawPassword.isBlank()) {
            InputSanitizer.validatePasswordStrength(rawPassword);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        if (role != null && role != user.getRole()) {
            if (!isAdmin) {
                throw new BusinessRuleException("Only administrators can change account roles");
            }
            user.setRole(role);
        }
        user = userRepository.save(user);

        activityLogService.log("USER_UPDATED",
                "User updated: " + cleanEmail, actor);
        return user;
    }

    /**
     * Soft-deletes a user by deactivating the account so that
     * contribution and campaign history remains intact.
     */
    public void deactivateUser(Long userId, User actor) {
        User user = findById(userId);
        boolean isSelf = actor != null && user.getId().equals(actor.getId());
        boolean isAdmin = actor != null && actor.getRole() == Role.ADMIN;
        if (!isSelf && !isAdmin) {
            throw new BusinessRuleException("Only administrators or the account owner can deactivate this user");
        }
        user.setActive(false);
        userRepository.save(user);
        activityLogService.log("USER_DEACTIVATED",
                "User deactivated: " + user.getEmail(), actor);
        log.info("User {} deactivated account {}", actor.getEmail(), user.getEmail());
    }

    /**
     * Hard-delete — only used when the user has no contributions or campaigns.
     */
    public void deleteUser(Long userId, User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only administrators can delete users");
        }
        User user = findById(userId);
        if (!user.getCampaigns().isEmpty() || !user.getContributions().isEmpty()) {
            deactivateUser(userId, admin);
            return;
        }
        userRepository.delete(user);
        activityLogService.log("USER_DELETED",
                "User permanently deleted: " + user.getEmail(), admin);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
