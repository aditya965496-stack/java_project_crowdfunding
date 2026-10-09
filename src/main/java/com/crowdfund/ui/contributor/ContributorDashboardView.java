package com.crowdfund.ui.contributor;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.Contribution;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
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
import java.util.List;

/**
 * Contributor dashboard with contribution summary and supported campaigns.
 */
@Route(value = "contributor", layout = MainLayout.class)
@PageTitle("Contributor Dashboard | CrowdFund")
@RolesAllowed("CONTRIBUTOR")
public class ContributorDashboardView extends VerticalLayout {

    public ContributorDashboardView(ContributionService contributionService,
                                    AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);

        User contributor = authenticatedUser.get().orElseThrow();
        H2 heading = new H2("Welcome, " + contributor.getName());
        heading.getStyle().set("margin-top", "0");

        BigDecimal totalContributed = contributionService.totalByContributor(contributor);
        List<Contribution> contributions = contributionService.findByContributor(contributor);
        List<Campaign> supported = contributionService.campaignsSupportedBy(contributor);

        HorizontalLayout stats = new HorizontalLayout(
                createCard("Total Contributed", "₹" + totalContributed.toPlainString(), "#E91E63"),
                createCard("Contributions", String.valueOf(contributions.size()), "var(--lumo-primary-color)"),
                createCard("Campaigns Supported", String.valueOf(supported.size()), "#4CAF50")
        );
        stats.setWidthFull();
        stats.getStyle().set("flex-wrap", "wrap");

        // Supported campaigns
        H3 campaignsHeader = new H3("Campaigns You Support");
        VerticalLayout campaignList = new VerticalLayout();
        campaignList.setPadding(false);

        if (supported.isEmpty()) {
            campaignList.add(new Span("You haven't supported any campaigns yet. Browse active campaigns to get started!"));
        } else {
            for (Campaign c : supported) {
                Div card = new Div();
                card.getStyle()
                        .set("background", "var(--lumo-base-color)")
                        .set("border-radius", "var(--lumo-border-radius-l)")
                        .set("padding", "var(--lumo-space-m)")
                        .set("box-shadow", "var(--lumo-box-shadow-s)")
                        .set("width", "100%");

                H3 title = new H3(c.getTitle());
                title.getStyle().set("margin", "0 0 var(--lumo-space-xs) 0");

                int progress = c.getProgressPercentage();
                Div progressBar = new Div();
                progressBar.getStyle()
                        .set("background", "var(--lumo-contrast-10pct)")
                        .set("border-radius", "4px")
                        .set("height", "6px")
                        .set("margin", "var(--lumo-space-xs) 0");
                Div fill = new Div();
                fill.getStyle()
                        .set("background", "var(--lumo-primary-color)")
                        .set("border-radius", "4px")
                        .set("height", "100%")
                        .set("width", Math.min(progress, 100) + "%");
                progressBar.add(fill);

                Span info = new Span("₹" + c.getRaisedAmount().toPlainString()
                        + " / ₹" + c.getGoalAmount().toPlainString()
                        + " • " + c.getStatus().name());
                info.getStyle()
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("color", "var(--lumo-secondary-text-color)");

                card.add(title, progressBar, info);
                campaignList.add(card);
            }
        }

        add(heading, stats, campaignsHeader, campaignList);
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
}
