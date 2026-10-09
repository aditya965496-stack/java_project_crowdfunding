package com.crowdfund.security;

import com.crowdfund.entity.User;
import com.crowdfund.repository.UserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility that resolves the currently authenticated {@link User} entity
 * from the Spring Security context.
 */
@Component
public class AuthenticatedUser {

    private final UserRepository userRepository;
    private final AuthenticationContext authContext;

    public AuthenticatedUser(UserRepository userRepository,
                             AuthenticationContext authContext) {
        this.userRepository = userRepository;
        this.authContext = authContext;
    }

    /**
     * Returns the currently logged-in user entity, or empty if anonymous.
     */
    public Optional<User> get() {
        return authContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(ud -> userRepository.findByEmail(ud.getUsername()));
    }

    public void logout() {
        authContext.logout();
    }
}
