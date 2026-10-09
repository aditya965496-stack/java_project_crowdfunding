package com.crowdfund.ui.contributor;

import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.ContributionService;
import com.crowdfund.service.UserService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

/**
 * Profile management page for contributors with contribution summary.
 */
@Route(value = "contributor/profile", layout = MainLayout.class)
@PageTitle("Profile | CrowdFund")
@RolesAllowed("CONTRIBUTOR")
public class ProfileView extends VerticalLayout {

    public ProfileView(UserService userService,
                       ContributionService contributionService,
                       AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);

        User user = authenticatedUser.get().orElseThrow();

        H2 heading = new H2("My Profile");

        // Summary
        BigDecimal totalContributed = contributionService.totalByContributor(user);
        int campaignsSupported = contributionService.campaignsSupportedBy(user).size();

        Span summary = new Span("Total contributed: ₹" + totalContributed.toPlainString()
                + " across " + campaignsSupported + " campaigns");
        summary.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-m)");

        // Edit form
        TextField nameField = new TextField("Full Name");
        nameField.setValue(user.getName());
        nameField.setWidthFull();

        EmailField emailField = new EmailField("Email");
        emailField.setValue(user.getEmail());
        emailField.setWidthFull();

        PasswordField passwordField = new PasswordField("New Password (leave blank to keep)");
        passwordField.setWidthFull();

        FormLayout form = new FormLayout(nameField, emailField, passwordField);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.setMaxWidth("600px");

        Button saveBtn = new Button("Update Profile", event -> {
            try {
                userService.updateUser(user.getId(),
                        nameField.getValue().trim(),
                        emailField.getValue().trim(),
                        passwordField.getValue().isBlank() ? null : passwordField.getValue(),
                        null, // keep same role
                        user);
                Notification.show("Profile updated successfully",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show(e.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(heading, summary, form, saveBtn);
    }
}
