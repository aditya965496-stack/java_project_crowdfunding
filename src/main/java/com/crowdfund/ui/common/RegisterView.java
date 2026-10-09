package com.crowdfund.ui.common;

import com.crowdfund.entity.Role;
import com.crowdfund.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Public registration page. Users choose Creator or Contributor roles.
 * Admin accounts are seeded or created by an existing admin.
 */
@Route("register")
@PageTitle("Register | CrowdFund")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H2 title = new H2("Create Your Account");

        TextField nameField = new TextField("Full Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        EmailField emailField = new EmailField("Email");
        emailField.setRequired(true);
        emailField.setWidthFull();

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setRequired(true);
        passwordField.setMinLength(6);
        passwordField.setWidthFull();

        PasswordField confirmField = new PasswordField("Confirm Password");
        confirmField.setRequired(true);
        confirmField.setWidthFull();

        ComboBox<Role> roleBox = new ComboBox<>("I want to");
        roleBox.setItems(Role.CREATOR, Role.CONTRIBUTOR);
        roleBox.setItemLabelGenerator(r ->
                r == Role.CREATOR ? "Create Campaigns" : "Support Campaigns");
        roleBox.setRequired(true);
        roleBox.setWidthFull();

        Button registerBtn = new Button("Register", event -> {
            if (nameField.isEmpty() || emailField.isEmpty()
                    || passwordField.isEmpty() || roleBox.isEmpty()) {
                Notification.show("Please fill in all fields",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (!passwordField.getValue().equals(confirmField.getValue())) {
                Notification.show("Passwords do not match",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (passwordField.getValue().length() < 6) {
                Notification.show("Password must be at least 6 characters",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                userService.register(
                        nameField.getValue().trim(),
                        emailField.getValue().trim(),
                        passwordField.getValue(),
                        roleBox.getValue()
                );
                Notification.show("Registration successful! Please log in.",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("login"));
            } catch (Exception e) {
                Notification.show(e.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setWidthFull();

        FormLayout form = new FormLayout();
        form.add(nameField, emailField, passwordField, confirmField, roleBox);
        form.setMaxWidth("400px");
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Span loginLink = new Span("Already have an account? ");
        RouterLink link = new RouterLink("Log in", LoginView.class);

        VerticalLayout card = new VerticalLayout(title, form, registerBtn,
                new Span(loginLink, link));
        card.setAlignItems(Alignment.CENTER);
        card.setMaxWidth("450px");
        card.getStyle()
                .set("padding", "var(--lumo-space-xl)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)");

        add(card);
    }
}
