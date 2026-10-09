package com.crowdfund.ui.creator;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.CampaignUpdate;
import com.crowdfund.entity.Contribution;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.ContributionService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lists the creator's campaigns with actions to manage, update,
 * and message contributors.
 */
@Route(value = "creator/campaigns", layout = MainLayout.class)
@PageTitle("My Campaigns | CrowdFund")
@RolesAllowed("CREATOR")
public class CreatorCampaignsView extends VerticalLayout {

    private final CampaignService campaignService;
    private final ContributionService contributionService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<Campaign> grid = new Grid<>(Campaign.class, false);

    public CreatorCampaignsView(CampaignService campaignService,
                                ContributionService contributionService,
                                AuthenticatedUser authenticatedUser) {
        this.campaignService = campaignService;
        this.contributionService = contributionService;
        this.authenticatedUser = authenticatedUser;

        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("My Campaigns");

        Button newCampaignBtn = new Button("New Campaign", e ->
                getUI().ifPresent(ui -> ui.navigate("creator/campaigns/new")));
        newCampaignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        configureGrid();
        refreshGrid();

        add(heading, newCampaignBtn, grid);
    }

    private void configureGrid() {
        grid.addColumn(Campaign::getTitle).setHeader("Title").setSortable(true);
        grid.addColumn(c -> c.getCategory() != null ? c.getCategory() : "")
                .setHeader("Category");
        grid.addColumn(c -> "₹" + c.getGoalAmount().toPlainString()).setHeader("Goal");
        grid.addColumn(c -> "₹" + c.getRaisedAmount().toPlainString()).setHeader("Raised");
        grid.addColumn(c -> c.getProgressPercentage() + "%").setHeader("Progress");
        grid.addColumn(new ComponentRenderer<>(c -> {
            Span badge = new Span(c.getStatus().name());
            badge.getElement().getThemeList().add("badge small");
            switch (c.getStatus()) {
                case ACTIVE -> badge.getElement().getThemeList().add("success");
                case PENDING -> badge.getElement().getThemeList().add("contrast");
                case COMPLETED -> badge.getElement().getThemeList().add("primary");
                case REJECTED, EXPIRED -> badge.getElement().getThemeList().add("error");
                default -> {}
            }
            return badge;
        })).setHeader("Status");
        grid.addColumn(c -> c.getDeadline().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setHeader("Deadline");
        grid.addColumn(new ComponentRenderer<>(campaign -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button("Edit");
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.addClickListener(e -> openEditDialog(campaign));

            Button updateBtn = new Button("Post Update");
            updateBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            updateBtn.addClickListener(e -> openPostUpdateDialog(campaign));

            Button msgBtn = new Button("Message");
            msgBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            msgBtn.addClickListener(e -> openMessageDialog(campaign));

            Button detailBtn = new Button("Details");
            detailBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            detailBtn.addClickListener(e -> openDetailsDialog(campaign));

            actions.add(editBtn, updateBtn, msgBtn, detailBtn);
            return actions;
        })).setHeader("Actions").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
    }

    private void refreshGrid() {
        User creator = authenticatedUser.get().orElseThrow();
        grid.setItems(campaignService.findByCreator(creator));
    }

    private void openEditDialog(Campaign campaign) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Campaign");
        dialog.setWidth("500px");

        TextField titleField = new TextField("Title");
        titleField.setValue(campaign.getTitle());
        titleField.setWidthFull();

        TextArea descField = new TextArea("Description");
        descField.setValue(campaign.getDescription());
        descField.setWidthFull();
        descField.setHeight("120px");

        TextField categoryField = new TextField("Category");
        categoryField.setValue(campaign.getCategory() != null ? campaign.getCategory() : "");
        categoryField.setWidthFull();

        FormLayout form = new FormLayout(titleField, descField, categoryField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button saveBtn = new Button("Save", e -> {
            try {
                User creator = authenticatedUser.get().orElseThrow();
                campaignService.updateCampaign(campaign.getId(),
                        titleField.getValue().trim(),
                        descField.getValue().trim(),
                        categoryField.getValue().trim(),
                        creator);
                Notification.show("Campaign updated",
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
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openPostUpdateDialog(Campaign campaign) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Post Update: " + campaign.getTitle());
        dialog.setWidth("500px");

        TextArea contentField = new TextArea("Update Content");
        contentField.setWidthFull();
        contentField.setHeight("120px");
        contentField.setPlaceholder("Share progress with your supporters...");
        dialog.add(contentField);

        Button postBtn = new Button("Post Update", e -> {
            if (contentField.isEmpty()) {
                Notification.show("Please enter update content",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                User creator = authenticatedUser.get().orElseThrow();
                campaignService.postUpdate(campaign.getId(),
                        contentField.getValue().trim(), creator);
                Notification.show("Update posted!",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        postBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, postBtn);
        dialog.open();
    }

    private void openMessageDialog(Campaign campaign) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Message Contributors: " + campaign.getTitle());
        dialog.setWidth("500px");

        TextArea contentField = new TextArea("Message");
        contentField.setWidthFull();
        contentField.setHeight("120px");
        contentField.setPlaceholder("Write a message to all supporters...");
        dialog.add(contentField);

        Button sendBtn = new Button("Send Message", e -> {
            if (contentField.isEmpty()) {
                Notification.show("Please enter a message",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                User creator = authenticatedUser.get().orElseThrow();
                campaignService.sendMessage(campaign.getId(),
                        contentField.getValue().trim(), creator);
                Notification.show("Message sent!",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, sendBtn);
        dialog.open();
    }

    private void openDetailsDialog(Campaign campaign) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(campaign.getTitle());
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);

        // Progress bar
        int progress = campaign.getProgressPercentage();
        Div progressBar = new Div();
        progressBar.getStyle()
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "4px")
                .set("height", "12px")
                .set("width", "100%");
        Div fill = new Div();
        fill.getStyle()
                .set("background", "var(--lumo-primary-color)")
                .set("border-radius", "4px")
                .set("height", "100%")
                .set("width", Math.min(progress, 100) + "%");
        progressBar.add(fill);

        content.add(new Span("₹" + campaign.getRaisedAmount().toPlainString()
                + " raised of ₹" + campaign.getGoalAmount().toPlainString()
                + " (" + progress + "%)"));
        content.add(progressBar);

        // Recent updates
        H3 updatesHeader = new H3("Recent Updates");
        List<CampaignUpdate> updates = campaignService.getUpdates(campaign.getId());
        if (updates.isEmpty()) {
            content.add(updatesHeader, new Span("No updates yet."));
        } else {
            content.add(updatesHeader);
            for (CampaignUpdate update : updates) {
                Div updateCard = new Div();
                updateCard.getStyle()
                        .set("background", "var(--lumo-contrast-5pct)")
                        .set("border-radius", "var(--lumo-border-radius-m)")
                        .set("padding", "var(--lumo-space-s)")
                        .set("margin-bottom", "var(--lumo-space-xs)");
                updateCard.add(new Span(update.getContent()));
                Span time = new Span(" — " + update.getPostedAt()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
                time.getStyle().set("color", "var(--lumo-secondary-text-color)")
                        .set("font-size", "var(--lumo-font-size-xs)");
                updateCard.add(time);
                content.add(updateCard);
            }
        }

        // Recent contributions
        H3 contribHeader = new H3("Contributions");
        List<Contribution> contributions = contributionService.findByCampaign(campaign);
        if (contributions.isEmpty()) {
            content.add(contribHeader, new Span("No contributions yet."));
        } else {
            content.add(contribHeader);
            Grid<Contribution> contribGrid = new Grid<>(Contribution.class, false);
            contribGrid.addColumn(c -> c.getContributor().getName()).setHeader("Contributor");
            contribGrid.addColumn(c -> "₹" + c.getAmount().toPlainString()).setHeader("Amount");
            contribGrid.addColumn(c -> c.getPaymentMethod() != null ? c.getPaymentMethod() : "N/A")
                    .setHeader("Method");
            contribGrid.addColumn(c -> c.getContributedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM HH:mm")))
                    .setHeader("Date");
            contribGrid.setItems(contributions);
            contribGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
            contribGrid.setAllRowsVisible(true);
            content.add(contribGrid);
        }

        dialog.add(content);
        Button closeBtn = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }
}
