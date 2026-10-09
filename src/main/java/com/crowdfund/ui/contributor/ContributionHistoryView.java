package com.crowdfund.ui.contributor;

import com.crowdfund.entity.Contribution;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.ContributionService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Displays the contributor's full contribution history with UPI details, UTR numbers,
 * verification statuses, and downloadable receipts.
 */
@Route(value = "contributor/history", layout = MainLayout.class)
@PageTitle("Contribution History | CrowdFund")
@RolesAllowed("CONTRIBUTOR")
public class ContributionHistoryView extends VerticalLayout {

    public ContributionHistoryView(ContributionService contributionService,
                                   AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);

        User contributor = authenticatedUser.get().orElseThrow();

        H2 heading = new H2("Contribution History");
        heading.getStyle().set("margin-top", "0");

        BigDecimal total = contributionService.totalByContributor(contributor);
        Span totalSpan = new Span("Total verified contributions: ₹" + (total != null ? total.toPlainString() : "0.00"));
        totalSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-color)");

        Grid<Contribution> grid = new Grid<>(Contribution.class, false);
        grid.addColumn(c -> c.getCampaign() != null ? c.getCampaign().getTitle() : "N/A")
                .setHeader("Campaign").setSortable(true).setWidth("200px");

        grid.addColumn(c -> "₹" + c.getAmount().toPlainString())
                .setHeader("Amount").setSortable(true).setWidth("110px");

        grid.addColumn(c -> c.getPaymentMethod() != null ? c.getPaymentMethod() : "UPI")
                .setHeader("Payment Method").setWidth("120px");

        grid.addColumn(c -> c.getUtrNumber() != null ? c.getUtrNumber() : "-")
                .setHeader("UTR / Ref No.").setWidth("160px");

        grid.addColumn(new ComponentRenderer<>(c -> {
            Span badge = new Span();
            badge.getStyle().set("padding", "3px 8px").set("border-radius", "4px").set("font-size", "12px").set("font-weight", "600");

            if (c.getStatus() == null) {
                badge.setText("N/A");
                return badge;
            }

            switch (c.getStatus()) {
                case SUCCESS -> {
                    badge.setText("Verified & Approved");
                    badge.getStyle().set("background", "#dcfce7").set("color", "#15803d");
                }
                case PENDING_VERIFICATION -> {
                    badge.setText("Pending Verification");
                    badge.getStyle().set("background", "#fef9c3").set("color", "#854d0e");
                }
                case REJECTED -> {
                    badge.setText("Rejected" + (c.getAdminNotes() != null ? ": " + c.getAdminNotes() : ""));
                    badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
                }
                case FAILED -> {
                    badge.setText("Failed");
                    badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
                }
                case PENDING -> {
                    badge.setText("Pending");
                    badge.getStyle().set("background", "#f1f5f9").set("color", "#475569");
                }
            }
            return badge;
        })).setHeader("Verification Status").setWidth("180px");

        grid.addColumn(c -> c.getContributedAt() != null
                ? c.getContributedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                : "-")
                .setHeader("Date").setSortable(true).setWidth("150px");

        grid.addColumn(new ComponentRenderer<>(c -> {
            if (c.getUtrNumber() != null && !c.getUtrNumber().isBlank()) {
                Anchor receiptLink = new Anchor("/api/servlet/receipt?utr=" + c.getUtrNumber(), "View Receipt");
                receiptLink.setTarget("_blank");
                receiptLink.getStyle().set("font-size", "13px").set("font-weight", "600").set("color", "#2563eb");
                return receiptLink;
            }
            return new Span("-");
        })).setHeader("Action").setWidth("130px");

        grid.setItems(contributionService.findByContributor(contributor));
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();

        add(heading, totalSpan, grid);
    }
}
