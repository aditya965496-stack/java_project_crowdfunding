package com.crowdfund.ui.creator;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.CampaignStatus;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.ContributionService;
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
import java.math.RoundingMode;
import java.util.List;

/**
 * Creator's dashboard with campaign analytics and summary statistics.
 */
@Route(value = "creator", layout = MainLayout.class)
@PageTitle("Creator Dashboard | CrowdFund")
@RolesAllowed("CREATOR")
public class CreatorDashboardView extends VerticalLayout {

    public CreatorDashboardView(CampaignService campaignService,
                                ContributionService contributionService,
                                AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);

        User creator = authenticatedUser.get().orElseThrow();
        H2 heading = new H2("Welcome, " + creator.getName());
        heading.getStyle().set("margin-top", "0");

        List<Campaign> campaigns = campaignService.findByCreator(creator);

        BigDecimal totalRaised = campaigns.stream()
                .map(Campaign::getRaisedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGoal = campaigns.stream()
                .map(Campaign::getGoalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int overallProgress = totalGoal.compareTo(BigDecimal.ZERO) > 0
                ? totalRaised.multiply(BigDecimal.valueOf(100))
                    .divide(totalGoal, 0, RoundingMode.HALF_UP).intValue()
                : 0;
        long activeCampaigns = campaigns.stream()
                .filter(c -> c.getStatus() == CampaignStatus.ACTIVE).count();
        long pendingCampaigns = campaigns.stream()
                .filter(c -> c.getStatus() == CampaignStatus.PENDING).count();
        long totalContributors = campaigns.stream()
                .mapToLong(campaignService::countDistinctContributors).sum();

        HorizontalLayout stats = new HorizontalLayout(
                createCard("Total Campaigns", String.valueOf(campaigns.size()), "var(--lumo-primary-color)"),
                createCard("Active", String.valueOf(activeCampaigns), "#4CAF50"),
                createCard("Pending", String.valueOf(pendingCampaigns), "#FF9800"),
                createCard("Total Raised", "₹" + totalRaised.toPlainString(), "#E91E63")
        );
        stats.setWidthFull();
        stats.getStyle().set("flex-wrap", "wrap");

        HorizontalLayout stats2 = new HorizontalLayout(
                createCard("Total Goal", "₹" + totalGoal.toPlainString(), "#2196F3"),
                createCard("Overall Progress", overallProgress + "%", "#9C27B0"),
                createCard("Contributors", String.valueOf(totalContributors), "#00BCD4")
        );
        stats2.setWidthFull();
        stats2.getStyle().set("flex-wrap", "wrap");

        // Campaign progress list
        H3 campaignHeader = new H3("Your Campaigns");
        VerticalLayout campaignList = new VerticalLayout();
        campaignList.setSpacing(true);
        campaignList.setPadding(false);

        for (Campaign c : campaigns) {
            Div card = createCampaignProgressCard(c, campaignService);
            campaignList.add(card);
        }

        add(heading, stats, stats2, campaignHeader, campaignList);
    }

    private Div createCard(String label, String value, String color) {
        H3 val = new H3(value);
        val.getStyle().set("margin", "0").set("color", color).set("font-size", "1.5em");
        Span lbl = new Span(label);
        lbl.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        Div card = new Div(val, lbl);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("min-width", "150px")
                .set("flex", "1")
                .set("text-align", "center");
        return card;
    }

    private Div createCampaignProgressCard(Campaign c, CampaignService campaignService) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("width", "100%");

        H3 title = new H3(c.getTitle());
        title.getStyle().set("margin", "0 0 var(--lumo-space-xs) 0");

        Span statusBadge = new Span(c.getStatus().name());
        statusBadge.getElement().getThemeList().add("badge small");
        switch (c.getStatus()) {
            case ACTIVE -> statusBadge.getElement().getThemeList().add("success");
            case PENDING -> statusBadge.getElement().getThemeList().add("contrast");
            case COMPLETED -> statusBadge.getElement().getThemeList().add("primary");
            case REJECTED -> statusBadge.getElement().getThemeList().add("error");
            case EXPIRED -> statusBadge.getElement().getThemeList().add("error");
            default -> {}
        }

        int progress = c.getProgressPercentage();

        // Progress bar
        Div progressBar = new Div();
        progressBar.getStyle()
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("height", "8px")
                .set("margin", "var(--lumo-space-s) 0");
        Div progressFill = new Div();
        progressFill.getStyle()
                .set("background", "var(--lumo-primary-color)")
                .set("border-radius", "4px")
                .set("height", "100%")
                .set("width", Math.min(progress, 100) + "%")
                .set("transition", "width 0.3s ease");
        progressBar.add(progressFill);

        Span info = new Span("₹" + c.getRaisedAmount().toPlainString()
                + " / ₹" + c.getGoalAmount().toPlainString()
                + " (" + progress + "%) • "
                + campaignService.countDistinctContributors(c) + " contributors"
                + " • Deadline: " + c.getDeadline());
        info.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        card.add(title, statusBadge, progressBar, info);
        return card;
    }
}
