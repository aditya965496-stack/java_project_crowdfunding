package com.crowdfund.ui.common;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Login page backed by Spring Security form login.
 * <p>
 * After successful authentication, users are routed to their
 * role-specific dashboard via {@link MainLayout}'s after-navigation logic.
 * </p>
 */
@Route("login")
@PageTitle("Login | CrowdFund")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        com.vaadin.flow.component.login.LoginI18n i18n = com.vaadin.flow.component.login.LoginI18n.createDefault();
        i18n.getForm().setUsername("Email or Username");
        loginForm.setI18n(i18n);

        H1 title = new H1("CrowdFund");
        title.getStyle().set("color", "var(--lumo-primary-color)");

        Span subtitle = new Span("Sign in to your account");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Anchor registerLink = new Anchor("register", "Don't have an account? Register");
        registerLink.getStyle().set("margin-top", "var(--lumo-space-m)");

        add(title, subtitle, loginForm, registerLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
