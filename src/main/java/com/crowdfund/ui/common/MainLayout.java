package com.crowdfund.ui.common;

import com.crowdfund.entity.Role;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;

import java.util.Optional;

/**
 * Application shell with a responsive side navigation drawer.
 * <p>
 * The navigation items shown depend on the logged-in user's role.
 * After login, if the user lands on the root path, they are
 * redirected to their role-specific dashboard.
 * </p>
 */
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final AuthenticatedUser authenticatedUser;

    public MainLayout(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        H1 appName = new H1("CrowdFund");
        appName.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("line-height", "1");

        HorizontalLayout header = new HorizontalLayout(toggle, appName);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(appName);
        header.setWidthFull();
        header.getStyle().set("padding", "0 var(--lumo-space-m)");

        // Logout button
        authenticatedUser.get().ifPresent(user -> {
            Span userName = new Span(user.getName());
            userName.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");

            Button logoutBtn = new Button("Logout", e -> authenticatedUser.logout());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            header.add(userName, logoutBtn);
        });

        addToNavbar(true, header);
    }

    private void addDrawerContent() {
        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isEmpty()) return;

        User user = maybeUser.get();
        SideNav nav = new SideNav();

        if (user.getRole() == Role.ADMIN) {
            nav.addItem(new SideNavItem("Dashboard", "admin",
                    VaadinIcon.DASHBOARD.create()));
            nav.addItem(new SideNavItem("Payment Verifications", "admin/payments",
                    VaadinIcon.MONEY_DEPOSIT.create()));
            nav.addItem(new SideNavItem("Users", "admin/users",
                    VaadinIcon.USERS.create()));
            nav.addItem(new SideNavItem("Campaigns", "admin/campaigns",
                    VaadinIcon.CLIPBOARD_TEXT.create()));
            nav.addItem(new SideNavItem("Settings", "admin/settings",
                    VaadinIcon.COG.create()));
            nav.addItem(new SideNavItem("Activity", "admin/activity",
                    VaadinIcon.BELL.create()));
        } else if (user.getRole() == Role.CREATOR) {
            nav.addItem(new SideNavItem("Dashboard", "creator",
                    VaadinIcon.DASHBOARD.create()));
            nav.addItem(new SideNavItem("My Campaigns", "creator/campaigns",
                    VaadinIcon.CLIPBOARD_TEXT.create()));
            nav.addItem(new SideNavItem("Create Campaign", "creator/campaigns/new",
                    VaadinIcon.PLUS_CIRCLE.create()));
            nav.addItem(new SideNavItem("Messages", "creator/messages",
                    VaadinIcon.ENVELOPE.create()));
        } else {
            nav.addItem(new SideNavItem("Dashboard", "contributor",
                    VaadinIcon.DASHBOARD.create()));
            nav.addItem(new SideNavItem("Browse Campaigns", "contributor/browse",
                    VaadinIcon.SEARCH.create()));
            nav.addItem(new SideNavItem("My Contributions", "contributor/history",
                    VaadinIcon.WALLET.create()));
            nav.addItem(new SideNavItem("Updates", "contributor/updates",
                    VaadinIcon.BELL.create()));
            nav.addItem(new SideNavItem("Profile", "contributor/profile",
                    VaadinIcon.USER.create()));
        }

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("scroller");

        Span roleBadge = new Span(user.getRole().name());
        roleBadge.getElement().getThemeList().add("badge");
        Footer footer = new Footer(roleBadge);

        addToDrawer(scroller, footer);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Redirect root to role dashboard
        String path = event.getLocation().getPath();
        if (path.isEmpty() || path.equals("/")) {
            authenticatedUser.get().ifPresent(user -> {
                String target = switch (user.getRole()) {
                    case ADMIN -> "admin";
                    case CREATOR -> "creator";
                    case CONTRIBUTOR -> "contributor";
                };
                getUI().ifPresent(ui -> ui.navigate(target));
            });
        }
    }
}
