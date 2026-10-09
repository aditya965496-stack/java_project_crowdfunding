package com.crowdfund.ui.common;

import com.crowdfund.entity.Role;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.Optional;

/**
 * Root route ("") entry point.
 * Dispatches unauthenticated visitors to /login and authenticated users to their role-specific dashboard.
 */
@Route("")
@AnonymousAllowed
public class RootView extends Div implements BeforeEnterObserver {

    private final AuthenticatedUser authenticatedUser;

    public RootView(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isEmpty()) {
            event.forwardTo(LoginView.class);
            return;
        }

        User user = maybeUser.get();
        String target = switch (user.getRole()) {
            case ADMIN -> "admin";
            case CREATOR -> "creator";
            case CONTRIBUTOR -> "contributor";
        };
        event.forwardTo(target);
    }
}
