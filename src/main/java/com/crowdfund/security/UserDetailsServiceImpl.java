package com.crowdfund.security;

import com.crowdfund.entity.User;
import com.crowdfund.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Loads user details from the database for Spring Security authentication.
 * <p>
 * The email address is used as the primary identifier. If "admin" is entered,
 * it resolves to the configured administrator account.
 * </p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Value("${crowdfund.security.admin.email:admin@crowdfund.com}")
    private String adminEmail;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Identifier cannot be blank");
        }
        String trimmed = identifier.trim();
        Optional<User> userOpt = userRepository.findByEmail(trimmed);
        if (userOpt.isEmpty() && trimmed.equalsIgnoreCase("admin")) {
            userOpt = userRepository.findByEmail(adminEmail);
        }

        User user = userOpt.orElseThrow(() -> new UsernameNotFoundException(
                "No user found with username or email: " + trimmed));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),   // enabled
                true,              // accountNonExpired
                true,              // credentialsNonExpired
                true,              // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
