package com.crowdfund.ui.creator;

import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.FileUploadService;
import com.crowdfund.service.SystemSettingService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mobile-responsive and network-resilient form for creating a fundraising campaign
 * with validated image uploads and double-submit prevention.
 */
@Route(value = "creator/campaigns/new", layout = MainLayout.class)
@PageTitle("Create Campaign | CrowdFund")
@RolesAllowed("CREATOR")
public class CampaignFormView extends VerticalLayout {

    public CampaignFormView(CampaignService campaignService,
                            SystemSettingService settingService,
                            FileUploadService fileUploadService,
                            AuthenticatedUser authenticatedUser) {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        setMaxWidth("800px");
        getStyle().set("margin", "0 auto");

        H2 heading = new H2("Create New Campaign");

        TextField titleField = new TextField("Campaign Title");
        titleField.setRequired(true);
        titleField.setWidthFull();
        titleField.setMaxLength(200);

        TextArea descField = new TextArea("Description");
        descField.setRequired(true);
        descField.setWidthFull();
        descField.setMaxLength(5000);
        descField.setHeight("150px");

        NumberField goalField = new NumberField("Goal Amount (₹)");
        goalField.setRequired(true);
        goalField.setMin(1);
        goalField.setMax(100000000);
        goalField.setWidthFull();

        DatePicker deadlinePicker = new DatePicker("Deadline");
        deadlinePicker.setRequired(true);
        deadlinePicker.setMin(LocalDate.now().plusDays(1));
        int maxDays = settingService.getMaxCampaignDurationDays();
        deadlinePicker.setMax(LocalDate.now().plusDays(maxDays));
        deadlinePicker.setWidthFull();

        TextField categoryField = new TextField("Category");
        categoryField.setWidthFull();
        categoryField.setPlaceholder("e.g., Education, Health, Technology");

        // ── Image Upload Component ─────────────────────────────────────
        AtomicReference<String> uploadedImageUrl = new AtomicReference<>(null);
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp", ".jpg", ".jpeg", ".png", ".webp");
        upload.setMaxFileSize(5 * 1024 * 1024); // 5 MB max
        upload.setWidthFull();

        Span uploadStatus = new Span("Optional: Upload campaign cover image (max 5 MB, JPEG/PNG/WebP)");
        uploadStatus.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout uploadSection = new VerticalLayout(uploadStatus, upload);
        uploadSection.setPadding(false);
        uploadSection.setSpacing(true);

        upload.addSucceededListener(event -> {
            try {
                InputStream is = buffer.getInputStream();
                String savedPath = fileUploadService.storeCampaignImage(
                        is,
                        event.getFileName(),
                        event.getMIMEType(),
                        event.getContentLength()
                );
                uploadedImageUrl.set(savedPath);
                uploadStatus.setText("✓ Cover image uploaded: " + event.getFileName());
                uploadStatus.getStyle().set("color", "var(--lumo-success-color)");
            } catch (Exception e) {
                Notification.show("Upload failed: " + e.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFileRejectedListener(event -> {
            Notification.show("File rejected: " + event.getErrorMessage(), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        FormLayout form = new FormLayout(titleField, descField, goalField,
                deadlinePicker, categoryField, uploadSection);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        form.setColspan(descField, 2);
        form.setColspan(uploadSection, 2);
        form.setWidthFull();

        // ── Submit Button with Slow Internet Protection ─────────────────
        Button submitBtn = new Button("Submit Campaign");
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.setDisableOnClick(true); // Prevents duplicate submits during slow network

        submitBtn.addClickListener(event -> {
            if (titleField.isEmpty() || descField.isEmpty()
                    || goalField.isEmpty() || deadlinePicker.isEmpty()) {
                Notification.show("Please fill in all required fields",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                submitBtn.setEnabled(true);
                return;
            }
            try {
                User creator = authenticatedUser.get().orElseThrow();
                campaignService.createCampaign(
                        titleField.getValue().trim(),
                        descField.getValue().trim(),
                        BigDecimal.valueOf(goalField.getValue()),
                        deadlinePicker.getValue(),
                        categoryField.getValue() != null ? categoryField.getValue().trim() : "",
                        uploadedImageUrl.get(),
                        creator
                );
                Notification.show("Campaign submitted for approval!",
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("creator/campaigns"));
            } catch (Exception e) {
                submitBtn.setEnabled(true);
                Notification.show(e.getMessage(),
                        4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        add(heading, form, submitBtn);
    }
}
