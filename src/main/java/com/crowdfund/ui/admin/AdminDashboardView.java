package com.crowdfund.ui.admin;

import com.crowdfund.entity.CampaignStatus;
import com.crowdfund.entity.Role;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.ContributionService;
import com.crowdfund.service.UserService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

/**
 * Admin dashboard showing key platform metrics at a glance.
 */
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin Dashboard | CrowdFund")
@RolesAllowed("ADMIN")
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView(UserService userService,
                              CampaignService campaignService,
                              ContributionService contributionService,
                              AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("Admin Dashboard");
        heading.getStyle().set("margin-top", "0");

        // Stats cards
        long totalUsers = userService.countAll();
        long totalCreators = userService.countByRole(Role.CREATOR);
        long totalContributors = userService.countByRole(Role.CONTRIBUTOR);
        long pendingCampaigns = campaignService.countByStatus(CampaignStatus.PENDING);
        long activeCampaigns = campaignService.countByStatus(CampaignStatus.ACTIVE);
        long totalContributions = contributionService.count();
        BigDecimal totalRaised = campaignService.totalRaised();

        long pendingPayments = contributionService.countPendingVerifications();

        HorizontalLayout statsRow1 = new HorizontalLayout(
                createStatCard("Total Users", String.valueOf(totalUsers), "var(--lumo-primary-color)"),
                createStatCard("Creators", String.valueOf(totalCreators), "#2196F3"),
                createStatCard("Contributors", String.valueOf(totalContributors), "#4CAF50"),
                createStatCard("Pending Campaigns", String.valueOf(pendingCampaigns), "#FF9800"),
                createStatCard("Pending Payments", String.valueOf(pendingPayments), "#E65100")
        );
        statsRow1.setWidthFull();
        statsRow1.getStyle().set("flex-wrap", "wrap");

        HorizontalLayout statsRow2 = new HorizontalLayout(
                createStatCard("Active Campaigns", String.valueOf(activeCampaigns), "#9C27B0"),
                createStatCard("Total Contributions", String.valueOf(totalContributions), "#00BCD4"),
                createStatCard("Total Raised", "₹" + totalRaised.toPlainString(), "#E91E63"),
                createStatCard("Platform", "Online", "#4CAF50")
        );
        statsRow2.setWidthFull();
        statsRow2.getStyle().set("flex-wrap", "wrap");

        add(heading, statsRow1, statsRow2);
    }

    private Div createStatCard(String label, String value, String color) {
        H3 valueText = new H3(value);
        valueText.getStyle()
                .set("margin", "0")
                .set("color", color)
                .set("font-size", "1.8em");

        Span labelText = new Span(label);
        labelText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Div card = new Div(valueText, labelText);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("min-width", "180px")
                .set("flex", "1")
                .set("text-align", "center");
        return card;
    }
}
