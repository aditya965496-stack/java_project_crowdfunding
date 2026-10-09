package com.crowdfund.ui.admin;

import com.crowdfund.entity.Role;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.UserService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

/**
 * Full CRUD for managing platform users. Supports search, role badges,
 * inline edit/delete with confirmation, and user creation dialogs.
 */
@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("User Management | CrowdFund")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final TextField searchField = new TextField();

    public UserManagementView(UserService userService,
                              AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;

        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("User Management");

        // Toolbar
        searchField.setPlaceholder("Search by name or email...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("Add User", VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.addClickListener(e -> openUserDialog(null));

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setWidthFull();
        toolbar.expand(searchField);

        // Grid setup
        configureGrid();
        refreshGrid();

        add(heading, toolbar, grid);
    }

    private void configureGrid() {
        grid.addColumn(User::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
        grid.addColumn(User::getName).setHeader("Name").setSortable(true);
        grid.addColumn(User::getEmail).setHeader("Email").setSortable(true);
        grid.addColumn(new ComponentRenderer<>(user -> {
            Span badge = new Span(user.getRole().name());
            badge.getElement().getThemeList().add("badge");
            switch (user.getRole()) {
                case ADMIN -> badge.getElement().getThemeList().add("error");
                case CREATOR -> badge.getElement().getThemeList().add("success");
                case CONTRIBUTOR -> badge.getElement().getThemeList().add("contrast");
            }
            return badge;
        })).setHeader("Role").setWidth("120px");
        grid.addColumn(new ComponentRenderer<>(user -> {
            Span badge = new Span(user.isActive() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add("badge");
            if (!user.isActive()) {
                badge.getElement().getThemeList().add("error");
            }
            return badge;
        })).setHeader("Status").setWidth("100px");
        grid.addColumn(user -> user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "").setHeader("Joined").setWidth("120px");
        grid.addColumn(new ComponentRenderer<>(user -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.addClickListener(e -> openUserDialog(user));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> confirmDelete(user));

            return new HorizontalLayout(editBtn, deleteBtn);
        })).setHeader("Actions").setWidth("120px");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.setPageSize(20);
    }

    private void refreshGrid() {
        String search = searchField.getValue();
        if (search != null && !search.isBlank()) {
            grid.setItems(userService.search(search,
                    org.springframework.data.domain.Pageable.unpaged()).getContent());
        } else {
            grid.setItems(userService.findAll());
        }
    }

    private void openUserDialog(User existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Create User" : "Edit User");
        dialog.setWidth("450px");

        TextField nameField = new TextField("Full Name");
        nameField.setWidthFull();
        nameField.setValue(existing != null ? existing.getName() : "");

        EmailField emailField = new EmailField("Email");
        emailField.setWidthFull();
        emailField.setValue(existing != null ? existing.getEmail() : "");

        PasswordField passwordField = new PasswordField(
                existing == null ? "Password" : "New Password (leave blank to keep)");
        passwordField.setWidthFull();

        ComboBox<Role> roleBox = new ComboBox<>("Role");
        roleBox.setItems(Role.values());
        roleBox.setWidthFull();
        if (existing != null) roleBox.setValue(existing.getRole());

        FormLayout form = new FormLayout(nameField, emailField, passwordField, roleBox);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button saveBtn = new Button("Save", event -> {
            try {
                User admin = authenticatedUser.get().orElseThrow();
                if (existing == null) {
                    userService.createUser(nameField.getValue().trim(),
                            emailField.getValue().trim(),
                            passwordField.getValue(),
                            roleBox.getValue(), admin);
                    Notification.show("User created successfully",
                            3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    userService.updateUser(existing.getId(),
                            nameField.getValue().trim(),
                            emailField.getValue().trim(),
                            passwordField.getValue().isBlank() ? null : passwordField.getValue(),
                            roleBox.getValue(), admin);
                    Notification.show("User updated successfully",
                            3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
                dialog.close();
                refreshGrid();
            } catch (Exception e) {
                Notification.show(e.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void confirmDelete(User user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete User");
        dialog.setText("Are you sure you want to delete " + user.getName() +
                "? Users with contributions will be deactivated instead.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            try {
                User admin = authenticatedUser.get().orElseThrow();
                userService.deleteUser(user.getId(), admin);
                Notification.show("User deleted/deactivated",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }
}
