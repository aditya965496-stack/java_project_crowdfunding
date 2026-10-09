package com.crowdfund.ui.admin;

import com.crowdfund.entity.SystemSetting;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.ActivityLogService;
import com.crowdfund.service.SystemSettingService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Admin settings panel for editing platform-wide configuration.
 */
@Route(value = "admin/settings", layout = MainLayout.class)
@PageTitle("System Settings | CrowdFund")
@RolesAllowed("ADMIN")
public class SystemSettingsView extends VerticalLayout {

    private final SystemSettingService settingService;
    private final ActivityLogService activityLogService;
    private final AuthenticatedUser authenticatedUser;

    public SystemSettingsView(SystemSettingService settingService,
                              ActivityLogService activityLogService,
                              AuthenticatedUser authenticatedUser) {
        this.settingService = settingService;
        this.activityLogService = activityLogService;
        this.authenticatedUser = authenticatedUser;

        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("System Settings");

        TextField platformName = createField("Platform Name",
                settingService.getPlatformName());
        TextField minContribution = createField("Minimum Contribution Amount (₹)",
                settingService.getMinContributionAmount().toPlainString());
        TextField maxDuration = createField("Maximum Campaign Duration (days)",
                String.valueOf(settingService.getMaxCampaignDurationDays()));
        TextField feePercentage = createField("Platform Fee (%)",
                settingService.getPlatformFeePercentage().toPlainString());

        FormLayout form = new FormLayout(platformName, minContribution,
                maxDuration, feePercentage);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        form.setMaxWidth("700px");

        Button saveBtn = new Button("Save Settings", event -> {
            try {
                settingService.saveSetting("platform.name", platformName.getValue().trim());
                settingService.saveSetting("min.contribution.amount", minContribution.getValue().trim());
                settingService.saveSetting("max.campaign.duration.days", maxDuration.getValue().trim());
                settingService.saveSetting("platform.fee.percentage", feePercentage.getValue().trim());

                authenticatedUser.get().ifPresent(admin ->
                    activityLogService.log("SETTINGS_UPDATED",
                            "System settings updated by admin", admin));

                Notification.show("Settings saved successfully",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Error saving settings: " + e.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(heading, form, saveBtn);
    }

    private TextField createField(String label, String value) {
        TextField field = new TextField(label);
        field.setValue(value != null ? value : "");
        field.setWidthFull();
        return field;
    }
}
