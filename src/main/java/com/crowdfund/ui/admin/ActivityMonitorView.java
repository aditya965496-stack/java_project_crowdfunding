package com.crowdfund.ui.admin;

import com.crowdfund.entity.ActivityLog;
import com.crowdfund.service.ActivityLogService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Live activity feed for admins. Uses a scheduled executor to poll
 * for new activity every 5 seconds and push updates to the client
 * via Vaadin's server push.
 */
@Route(value = "admin/activity", layout = MainLayout.class)
@PageTitle("Activity Monitoring | CrowdFund")
@RolesAllowed("ADMIN")
public class ActivityMonitorView extends VerticalLayout {

    private final ActivityLogService activityLogService;
    private final Grid<ActivityLog> grid = new Grid<>(ActivityLog.class, false);
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> pollingTask;

    public ActivityMonitorView(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;

        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("Activity Monitoring");

        Button refreshBtn = new Button("Refresh Now", e -> refreshGrid());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Span liveIndicator = new Span("● Live");
        liveIndicator.getStyle()
                .set("color", "#4CAF50")
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)");

        configureGrid();
        refreshGrid();

        add(heading, liveIndicator, refreshBtn, grid);
    }

    private void configureGrid() {
        grid.addColumn(new ComponentRenderer<>(log -> {
            Span badge = new Span(log.getType());
            badge.getElement().getThemeList().add("badge small");
            String type = log.getType();
            if (type.contains("CREATED") || type.contains("REGISTERED")) {
                badge.getElement().getThemeList().add("success");
            } else if (type.contains("DELETED") || type.contains("REJECTED")
                    || type.contains("EXPIRED")) {
                badge.getElement().getThemeList().add("error");
            } else if (type.contains("CONTRIBUTION")) {
                badge.getElement().getThemeList().add("primary");
            } else {
                badge.getElement().getThemeList().add("contrast");
            }
            return badge;
        })).setHeader("Type").setWidth("180px");
        grid.addColumn(ActivityLog::getDescription).setHeader("Description");
        grid.addColumn(log -> {
            try {
                return (log.getActor() != null && log.getActor().getName() != null) ? log.getActor().getName() : "System";
            } catch (Exception e) {
                return "System";
            }
        }).setHeader("Actor");
        grid.addColumn(log -> log.getTimestamp()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")))
                .setHeader("Time").setWidth("180px");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
    }

    private void refreshGrid() {
        grid.setItems(activityLogService.getRecentActivity());
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        UI ui = event.getUI();
        executor = Executors.newSingleThreadScheduledExecutor();
        pollingTask = executor.scheduleAtFixedRate(() -> {
            ui.access(this::refreshGrid);
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        if (pollingTask != null) {
            pollingTask.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
