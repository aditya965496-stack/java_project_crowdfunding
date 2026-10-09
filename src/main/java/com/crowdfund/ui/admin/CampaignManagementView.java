package com.crowdfund.ui.admin;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.CampaignStatus;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;

/**
 * Admin view for reviewing, approving, and rejecting campaigns.
 */
@Route(value = "admin/campaigns", layout = MainLayout.class)
@PageTitle("Campaign Management | CrowdFund")
@RolesAllowed("ADMIN")
public class CampaignManagementView extends VerticalLayout {

    private final CampaignService campaignService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<Campaign> grid = new Grid<>(Campaign.class, false);
    private final ComboBox<CampaignStatus> statusFilter = new ComboBox<>("Filter by Status");

    public CampaignManagementView(CampaignService campaignService,
                                  AuthenticatedUser authenticatedUser) {
        this.campaignService = campaignService;
        this.authenticatedUser = authenticatedUser;

        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("Campaign Management");

        statusFilter.setItems(CampaignStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.setPlaceholder("All statuses");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        configureGrid();
        refreshGrid();

        add(heading, statusFilter, grid);
    }

    private void configureGrid() {
        grid.addColumn(Campaign::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
        grid.addColumn(Campaign::getTitle).setHeader("Title").setSortable(true);
        grid.addColumn(c -> {
            try {
                return (c.getCreator() != null && c.getCreator().getName() != null) ? c.getCreator().getName() : "";
            } catch (Exception e) {
                return "";
            }
        }).setHeader("Creator");
        grid.addColumn(c -> "₹" + c.getGoalAmount().toPlainString())
                .setHeader("Goal");
        grid.addColumn(c -> "₹" + c.getRaisedAmount().toPlainString())
                .setHeader("Raised");
        grid.addColumn(c -> c.getDeadline().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setHeader("Deadline");
        grid.addColumn(new ComponentRenderer<>(campaign -> {
            Span badge = new Span(campaign.getStatus().name());
            badge.getElement().getThemeList().add("badge");
            switch (campaign.getStatus()) {
                case PENDING -> badge.getElement().getThemeList().add("contrast");
                case ACTIVE, APPROVED -> badge.getElement().getThemeList().add("success");
                case COMPLETED -> badge.getElement().getThemeList().add("primary");
                case EXPIRED -> badge.getElement().getThemeList().add("error");
                case REJECTED -> badge.getElement().getThemeList().add("error");
            }
            return badge;
        })).setHeader("Status");
        grid.addColumn(new ComponentRenderer<>(campaign -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            if (campaign.getStatus() == CampaignStatus.PENDING) {
                Button approveBtn = new Button("Approve");
                approveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
                approveBtn.addClickListener(e -> {
                    try {
                        User admin = authenticatedUser.get().orElseThrow();
                        campaignService.approveCampaign(campaign.getId(), admin);
                        Notification.show("Campaign approved!",
                                3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        refreshGrid();
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage(),
                                4000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });

                Button rejectBtn = new Button("Reject");
                rejectBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                rejectBtn.addClickListener(e -> openRejectDialog(campaign));

                actions.add(approveBtn, rejectBtn);
            }

            Button viewBtn = new Button("View");
            viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewBtn.addClickListener(e -> openViewDialog(campaign));
            actions.add(viewBtn);

            return actions;
        })).setHeader("Actions").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
    }

    private void refreshGrid() {
        CampaignStatus status = statusFilter.getValue();
        if (status != null) {
            grid.setItems(campaignService.findByStatus(status, Pageable.unpaged()).getContent());
        } else {
            grid.setItems(campaignService.findAll(Pageable.unpaged()).getContent());
        }
    }

    private void openRejectDialog(Campaign campaign) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reject Campaign: " + campaign.getTitle());
        dialog.setWidth("400px");

        TextArea reasonField = new TextArea("Rejection Reason (optional)");
        reasonField.setWidthFull();
        dialog.add(reasonField);

        Button confirmBtn = new Button("Reject", e -> {
            try {
                User admin = authenticatedUser.get().orElseThrow();
                campaignService.rejectCampaign(campaign.getId(),
                        reasonField.getValue(), admin);
                Notification.show("Campaign rejected",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }

    private void openViewDialog(Campaign campaign) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(campaign.getTitle());
        dialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.add(new Span("Creator: " + (campaign.getCreator() != null
                ? campaign.getCreator().getName() : "N/A")));
        content.add(new Span("Category: " + campaign.getCategory()));
        content.add(new Span("Goal: ₹" + campaign.getGoalAmount().toPlainString()));
        content.add(new Span("Raised: ₹" + campaign.getRaisedAmount().toPlainString()));
        content.add(new Span("Progress: " + campaign.getProgressPercentage() + "%"));
        content.add(new Span("Deadline: " + campaign.getDeadline()));
        content.add(new Span("Status: " + campaign.getStatus()));
        if (campaign.getRejectionReason() != null && !campaign.getRejectionReason().isBlank()) {
            content.add(new Span("Rejection Reason: " + campaign.getRejectionReason()));
        }
        content.add(new Span("Description:"));
        Span desc = new Span(campaign.getDescription());
        desc.getStyle().set("white-space", "pre-wrap");
        content.add(desc);

        dialog.add(content);
        Button closeBtn = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }
}
