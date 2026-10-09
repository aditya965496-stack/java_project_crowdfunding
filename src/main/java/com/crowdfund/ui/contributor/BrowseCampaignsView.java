package com.crowdfund.ui.contributor;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.User;
import com.crowdfund.security.AuthenticatedUser;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.ContributionService;
import com.crowdfund.service.FileUploadService;
import com.crowdfund.service.SystemSettingService;
import com.crowdfund.ui.common.MainLayout;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Browse active campaigns and contribute to them via UPI payment.
 * Contributor transfers funds to UPI ID: adityalive49@oksbi, uploads payment screenshot,
 * and enters UTR number for verification by admin.
 */
@Route(value = "contributor/browse", layout = MainLayout.class)
@PageTitle("Browse Campaigns | CrowdFund")
@RolesAllowed("CONTRIBUTOR")
public class BrowseCampaignsView extends VerticalLayout {

    public static final String UPI_ID = "adityalive49@oksbi";

    private final CampaignService campaignService;
    private final ContributionService contributionService;
    private final SystemSettingService settingService;
    private final FileUploadService fileUploadService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<Campaign> grid = new Grid<>(Campaign.class, false);
    private final TextField searchField = new TextField();

    public BrowseCampaignsView(CampaignService campaignService,
                               ContributionService contributionService,
                               SystemSettingService settingService,
                               FileUploadService fileUploadService,
                               AuthenticatedUser authenticatedUser) {
        this.campaignService = campaignService;
        this.contributionService = contributionService;
        this.settingService = settingService;
        this.fileUploadService = fileUploadService;
        this.authenticatedUser = authenticatedUser;

        setPadding(true);
        setSpacing(true);

        H2 heading = new H2("Browse Active Campaigns");
        heading.getStyle().set("margin-top", "0");

        searchField.setPlaceholder("Search campaigns by title, description or category...");
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.setMaxWidth("450px");
        searchField.addValueChangeListener(e -> refreshGrid());

        configureGrid();
        refreshGrid();

        add(heading, searchField, grid);
    }

    private void configureGrid() {
        grid.addColumn(new ComponentRenderer<>(c -> {
            if (c.getImageUrl() != null && !c.getImageUrl().isBlank()) {
                Image img = new Image(c.getImageUrl(), c.getTitle());
                img.setWidth("48px");
                img.setHeight("48px");
                img.getStyle().set("border-radius", "6px").set("object-fit", "cover");
                return img;
            }
            Span placeholder = new Span("📷");
            placeholder.getStyle().set("font-size", "24px").set("opacity", "0.5");
            return placeholder;
        })).setHeader("Image").setWidth("70px").setFlexGrow(0);

        grid.addColumn(Campaign::getTitle).setHeader("Title").setSortable(true);
        grid.addColumn(c -> c.getCategory() != null ? c.getCategory() : "").setHeader("Category");
        grid.addColumn(c -> c.getCreator() != null ? c.getCreator().getName() : "").setHeader("Creator");
        grid.addColumn(c -> "₹" + c.getGoalAmount().toPlainString()).setHeader("Goal");
        grid.addColumn(c -> "₹" + c.getRaisedAmount().toPlainString()).setHeader("Raised");
        grid.addColumn(new ComponentRenderer<>(c -> {
            int progress = c.getProgressPercentage();
            Div bar = new Div();
            bar.getStyle()
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("border-radius", "4px")
                    .set("height", "6px")
                    .set("width", "80px");
            Div fill = new Div();
            fill.getStyle()
                    .set("background", "var(--lumo-primary-color)")
                    .set("border-radius", "4px")
                    .set("height", "100%")
                    .set("width", Math.min(progress, 100) + "%");
            bar.add(fill);
            VerticalLayout layout = new VerticalLayout(bar, new Span(progress + "%"));
            layout.setPadding(false);
            layout.setSpacing(false);
            return layout;
        })).setHeader("Progress");
        grid.addColumn(c -> c.getDeadline().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setHeader("Deadline");
        grid.addColumn(new ComponentRenderer<>(campaign -> {
            Button contributeBtn = new Button("Pay & Contribute", VaadinIcon.MONEY_DEPOSIT.create());
            contributeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            contributeBtn.addClickListener(e -> openUpiPaymentDialog(campaign));
            return contributeBtn;
        })).setHeader("Action").setWidth("170px");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
    }

    private void refreshGrid() {
        String search = searchField.getValue();
        grid.setItems(campaignService.searchActiveCampaigns(search, Pageable.unpaged()).getContent());
    }

    /**
     * Dialog for UPI Payment with UPI ID adityalive49@oksbi, UTR input, and payment screenshot proof.
     */
    private void openUpiPaymentDialog(Campaign campaign) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Support: " + campaign.getTitle());
        dialog.setWidth("min(520px, 96vw)");

        BigDecimal minAmount = settingService.getMinContributionAmount();

        NumberField amountField = new NumberField("1. Contribution Amount (₹)");
        amountField.setMin(minAmount.doubleValue());
        amountField.setValue(Math.max(500.0, minAmount.doubleValue()));
        amountField.setStep(10);
        amountField.setWidthFull();
        amountField.setHelperText("Minimum amount: ₹" + minAmount.toPlainString());

        // UPI Payment Info Box
        Div upiCard = new Div();
        upiCard.getStyle()
                .set("background", "#f0fdf4")
                .set("border", "1px solid #bbf7d0")
                .set("border-radius", "8px")
                .set("padding", "14px")
                .set("margin-top", "10px");

        Span upiTitle = new Span("2. Pay via UPI to Official ID:");
        upiTitle.getStyle().set("font-weight", "600").set("color", "#166534").set("display", "block");

        H3 upiIdText = new H3(UPI_ID);
        upiIdText.getStyle()
                .set("margin", "6px 0")
                .set("color", "#15803d")
                .set("font-family", "monospace")
                .set("letter-spacing", "0.5px");

        Button copyBtn = new Button("Copy UPI ID", VaadinIcon.COPY.create());
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        copyBtn.addClickListener(e -> {
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", UPI_ID);
            Notification.show("UPI ID copied: " + UPI_ID, 2500, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        HorizontalLayout upiRow = new HorizontalLayout(upiIdText, copyBtn);
        upiRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Paragraph instructions = new Paragraph("• Pay using Google Pay, PhonePe, Paytm, or BHIM.\n• After payment, enter your 12-digit UTR/Ref No. and upload the payment screenshot below.");
        instructions.getStyle().set("font-size", "12px").set("color", "#1e3a8a").set("margin", "6px 0 0 0").set("white-space", "pre-line");

        upiCard.add(upiTitle, upiRow, instructions);

        // UTR Field
        TextField utrField = new TextField("3. UPI Transaction Reference / UTR Number");
        utrField.setPlaceholder("e.g. 428719283741 (12 digits)");
        utrField.setRequired(true);
        utrField.setWidthFull();
        utrField.setHelperText("Found in your UPI app payment receipt / SMS");

        // Screenshot Upload
        AtomicReference<String> screenshotPathRef = new AtomicReference<>(null);
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp", ".jpg", ".jpeg", ".png", ".webp");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setWidthFull();

        Span uploadLabel = new Span("4. Upload Payment Screenshot (Proof)");
        uploadLabel.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-s)");

        Span uploadStatus = new Span("Upload screenshot (Max 5MB - JPG, PNG, WebP)");
        uploadStatus.getStyle().set("font-size", "12px").set("color", "var(--lumo-secondary-text-color)");

        Image previewImage = new Image();
        previewImage.setMaxHeight("120px");
        previewImage.setVisible(false);
        previewImage.getStyle().set("border-radius", "6px").set("border", "1px solid #e2e8f0").set("margin-top", "6px");

        upload.addSucceededListener(event -> {
            try {
                InputStream is = buffer.getInputStream();
                String savedPath = fileUploadService.storePaymentScreenshot(
                        is,
                        event.getFileName(),
                        event.getMIMEType(),
                        event.getContentLength()
                );
                screenshotPathRef.set(savedPath);
                uploadStatus.setText("✓ Screenshot uploaded: " + event.getFileName());
                uploadStatus.getStyle().set("color", "var(--lumo-success-color)");
                previewImage.setSrc(savedPath);
                previewImage.setVisible(true);
            } catch (Exception ex) {
                Notification.show("Screenshot upload failed: " + ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFileRejectedListener(event -> {
            Notification.show("File rejected: " + event.getErrorMessage(), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        VerticalLayout uploadLayout = new VerticalLayout(uploadLabel, upload, uploadStatus, previewImage);
        uploadLayout.setPadding(false);
        uploadLayout.setSpacing(false);

        VerticalLayout content = new VerticalLayout(amountField, upiCard, utrField, uploadLayout);
        content.setPadding(false);
        content.setSpacing(true);
        dialog.add(content);

        Button submitBtn = new Button("Submit for Admin Verification");
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.setDisableOnClick(true);

        submitBtn.addClickListener(e -> {
            if (amountField.isEmpty() || utrField.isEmpty()) {
                Notification.show("Please enter the contribution amount and UTR number.",
                        3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                submitBtn.setEnabled(true);
                return;
            }

            if (screenshotPathRef.get() == null) {
                Notification.show("Please upload the payment screenshot proof before submitting.",
                        3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                submitBtn.setEnabled(true);
                return;
            }

            try {
                User contributor = authenticatedUser.get().orElseThrow();
                contributionService.submitUpiContribution(
                        campaign.getId(),
                        contributor,
                        BigDecimal.valueOf(amountField.getValue()),
                        utrField.getValue().trim(),
                        screenshotPathRef.get()
                );

                Notification.show("Payment submitted successfully! Admin will verify your UTR and screenshot shortly.",
                        5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                submitBtn.setEnabled(true);
                Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, submitBtn);
        dialog.open();
    }
}
