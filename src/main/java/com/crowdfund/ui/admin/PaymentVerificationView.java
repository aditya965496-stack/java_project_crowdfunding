package com.crowdfund.ui.admin;

import com.crowdfund.entity.Contribution;
import com.crowdfund.entity.ContributionStatus;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.ContributionService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin portal view for verifying UPI payments submitted by contributors.
 * Allows Admin to inspect payer screenshot, verify UTR number, and Approve or Reject payments.
 */
@Route(value = "admin/payments", layout = MainLayout.class)
@PageTitle("Payment Verifications | CrowdFund Admin")
@RolesAllowed("ADMIN")
public class PaymentVerificationView extends VerticalLayout {

    private final ContributionService contributionService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<Contribution> grid = new Grid<>(Contribution.class, false);
    private final TextField searchField = new TextField();

    private ContributionStatus currentFilter = ContributionStatus.PENDING_VERIFICATION;
    private final Button btnPending = new Button("Pending Verification");
    private final Button btnSuccess = new Button("Approved");
    private final Button btnRejected = new Button("Rejected");
    private final Button btnAll = new Button("All Payments");

    public PaymentVerificationView(ContributionService contributionService,
                                   AuthenticatedUser authenticatedUser) {
        this.contributionService = contributionService;
        this.authenticatedUser = authenticatedUser;

        setPadding(true);
        setSpacing(true);

        H2 title = new H2("UPI Payment Verifications");
        title.getStyle().set("margin-top", "0").set("margin-bottom", "4px");

        Paragraph subtitle = new Paragraph("Review submitted contributions, inspect payment screenshots and UTR numbers, and approve or reject transactions.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        setupFilterButtons();
        setupSearch();
        configureGrid();
        refreshGrid();

        HorizontalLayout toolbar = new HorizontalLayout(btnPending, btnSuccess, btnRejected, btnAll, searchField);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.getStyle().set("flex-wrap", "wrap");

        add(title, subtitle, toolbar, grid);
    }

    private void setupFilterButtons() {
        btnPending.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnPending.addClickListener(e -> setFilter(ContributionStatus.PENDING_VERIFICATION, btnPending));
        btnSuccess.addClickListener(e -> setFilter(ContributionStatus.SUCCESS, btnSuccess));
        btnRejected.addClickListener(e -> setFilter(ContributionStatus.REJECTED, btnRejected));
        btnAll.addClickListener(e -> setFilter(null, btnAll));
    }

    private void setFilter(ContributionStatus status, Button activeBtn) {
        currentFilter = status;
        List.of(btnPending, btnSuccess, btnRejected, btnAll).forEach(b -> b.removeThemeVariants(ButtonVariant.LUMO_PRIMARY));
        activeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshGrid();
    }

    private void setupSearch() {
        searchField.setPlaceholder("Search by UTR, contributor, or campaign...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("320px");
        searchField.addValueChangeListener(e -> refreshGrid());
    }

    private void configureGrid() {
        grid.addColumn(c -> c.getContributedAt() != null
                ? c.getContributedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                : "-")
                .setHeader("Date & Time").setWidth("150px").setSortable(true);

        grid.addColumn(c -> c.getContributor() != null
                ? c.getContributor().getName() + " (" + c.getContributor().getEmail() + ")"
                : "-")
                .setHeader("Contributor").setWidth("220px");

        grid.addColumn(c -> c.getCampaign() != null ? c.getCampaign().getTitle() : "-")
                .setHeader("Campaign").setWidth("200px");

        grid.addColumn(c -> "₹" + c.getAmount().toPlainString())
                .setHeader("Amount").setWidth("110px").setSortable(true);

        grid.addColumn(Contribution::getUpiId)
                .setHeader("UPI ID").setWidth("170px");

        grid.addColumn(new ComponentRenderer<>(c -> {
            Span utrSpan = new Span(c.getUtrNumber() != null ? c.getUtrNumber() : "N/A");
            utrSpan.getStyle().set("font-family", "monospace").set("font-weight", "600");
            return utrSpan;
        })).setHeader("UTR Number").setWidth("160px");

        // Screenshot preview column
        grid.addColumn(new ComponentRenderer<>(c -> {
            if (c.getPaymentScreenshotPath() != null && !c.getPaymentScreenshotPath().isBlank()) {
                Button viewSsBtn = new Button("View Screenshot", VaadinIcon.PICTURE.create());
                viewSsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                viewSsBtn.addClickListener(e -> openScreenshotDialog(c));
                return viewSsBtn;
            }
            return new Span("No Screenshot");
        })).setHeader("Payment Proof").setWidth("160px");

        // Status badge column
        grid.addColumn(new ComponentRenderer<>(c -> {
            Span badge = new Span();
            badge.getStyle().set("padding", "4px 8px").set("border-radius", "4px").set("font-size", "12px").set("font-weight", "600");

            if (c.getStatus() == ContributionStatus.SUCCESS) {
                badge.setText("Verified & Approved");
                badge.getStyle().set("background", "#dcfce7").set("color", "#15803d");
            } else if (c.getStatus() == ContributionStatus.PENDING_VERIFICATION) {
                badge.setText("Pending Verification");
                badge.getStyle().set("background", "#fef9c3").set("color", "#854d0e");
            } else if (c.getStatus() == ContributionStatus.REJECTED) {
                badge.setText("Rejected");
                badge.getStyle().set("background", "#fee2e2").set("color", "#b91c1c");
            } else {
                badge.setText(c.getStatus().name());
            }
            return badge;
        })).setHeader("Status").setWidth("170px");

        // Actions column
        grid.addColumn(new ComponentRenderer<>(c -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            if (c.getStatus() == ContributionStatus.PENDING_VERIFICATION) {
                Button approveBtn = new Button("Approve", VaadinIcon.CHECK.create());
                approveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                approveBtn.addClickListener(e -> openApprovalDialog(c));

                Button rejectBtn = new Button("Reject", VaadinIcon.CLOSE.create());
                rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                rejectBtn.addClickListener(e -> openRejectDialog(c));

                actions.add(approveBtn, rejectBtn);
            } else {
                Anchor receiptLink = new Anchor("/api/servlet/receipt?utr=" + c.getUtrNumber(), "Receipt");
                receiptLink.setTarget("_blank");
                receiptLink.getStyle().set("font-size", "13px").set("font-weight", "600").set("color", "#2563eb");
                actions.add(receiptLink);
            }

            return actions;
        })).setHeader("Admin Action").setWidth("220px");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
    }

    private void refreshGrid() {
        List<Contribution> list;
        if (currentFilter != null) {
            list = contributionService.getContributionsByStatus(currentFilter);
        } else {
            list = contributionService.getAllContributions();
        }

        String search = searchField.getValue();
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase().trim();
            list = list.stream().filter(c ->
                    (c.getUtrNumber() != null && c.getUtrNumber().toLowerCase().contains(lower)) ||
                    (c.getContributor() != null && c.getContributor().getName().toLowerCase().contains(lower)) ||
                    (c.getContributor() != null && c.getContributor().getEmail().toLowerCase().contains(lower)) ||
                    (c.getCampaign() != null && c.getCampaign().getTitle().toLowerCase().contains(lower))
            ).collect(Collectors.toList());
        }

        grid.setItems(list);
    }

    private void openScreenshotDialog(Contribution c) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Payment Screenshot - UTR: " + c.getUtrNumber());
        dialog.setWidth("min(650px, 95vw)");

        Image img = new Image(c.getPaymentScreenshotPath(), "Payment Proof Screenshot");
        img.setWidthFull();
        img.getStyle().set("border-radius", "8px").set("object-fit", "contain").set("max-height", "450px");

        VerticalLayout content = new VerticalLayout(
                new Paragraph("Contributor: " + c.getContributor().getName() + " | Amount: ₹" + c.getAmount()),
                img
        );
        content.setPadding(false);
        dialog.add(content);

        Button closeBtn = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private void openApprovalDialog(Contribution c) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Confirm Payment Approval");

        VerticalLayout layout = new VerticalLayout(
                new Paragraph("Approve contribution of ₹" + c.getAmount() + " for campaign \"" + c.getCampaign().getTitle() + "\"?"),
                new Paragraph("UTR Number: " + c.getUtrNumber()),
                new Paragraph("This will credit the campaign raised amount immediately.")
        );
        dialog.add(layout);

        Button confirmBtn = new Button("Confirm & Approve", VaadinIcon.CHECK.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        confirmBtn.addClickListener(e -> {
            try {
                User admin = authenticatedUser.get().orElseThrow();
                contributionService.verifyPayment(c.getId(), admin, true, "Verified via UPI statement");
                Notification.show("Payment verified and approved! Campaign raised amount updated.",
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Approval failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void openRejectDialog(Contribution c) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reject Payment");

        TextField reasonField = new TextField("Rejection Reason");
        reasonField.setPlaceholder("e.g. UTR not found on bank statement / invalid screenshot");
        reasonField.setWidthFull();

        VerticalLayout layout = new VerticalLayout(
                new Paragraph("Are you sure you want to reject payment of ₹" + c.getAmount() + " (UTR: " + c.getUtrNumber() + ")?"),
                reasonField
        );
        dialog.add(layout);

        Button confirmBtn = new Button("Reject Payment", VaadinIcon.CLOSE.create());
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        confirmBtn.addClickListener(e -> {
            try {
                User admin = authenticatedUser.get().orElseThrow();
                contributionService.verifyPayment(c.getId(), admin, false, reasonField.getValue());
                Notification.show("Payment marked as rejected.", 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Rejection failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }
}
