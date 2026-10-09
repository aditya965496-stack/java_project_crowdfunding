package com.crowdfund.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a financial contribution from a Contributor to a Campaign.
 * Supports UPI payment verification workflow with UTR number and payment proof screenshot.
 */
@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contributor_id", nullable = false)
    private User contributor;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Size(max = 50)
    @Column(length = 50)
    private String paymentMethod = "UPI";

    @Size(max = 100)
    @Column(name = "upi_id", length = 100)
    private String upiId = "adityalive49@oksbi";

    @Size(max = 100)
    @Column(name = "utr_number", length = 100)
    private String utrNumber;

    @Size(max = 255)
    @Column(name = "payment_screenshot_path", length = 255)
    private String paymentScreenshotPath;

    @Size(max = 255)
    @Column(name = "admin_notes", length = 255)
    private String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Size(max = 100)
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Size(max = 100)
    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContributionStatus status = ContributionStatus.PENDING_VERIFICATION;

    @Column(nullable = false, updatable = false)
    private LocalDateTime contributedAt;

    @PrePersist
    protected void onCreate() {
        this.contributedAt = LocalDateTime.now();
        if (this.upiId == null || this.upiId.isBlank()) {
            this.upiId = "adityalive49@oksbi";
        }
    }

    // ── Constructors ──────────────────────────────────────────

    public Contribution() {
    }

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public User getContributor() {
        return contributor;
    }

    public void setContributor(User contributor) {
        this.contributor = contributor;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getUtrNumber() {
        return utrNumber;
    }

    public void setUtrNumber(String utrNumber) {
        this.utrNumber = utrNumber;
    }

    public String getPaymentScreenshotPath() {
        return paymentScreenshotPath;
    }

    public void setPaymentScreenshotPath(String paymentScreenshotPath) {
        this.paymentScreenshotPath = paymentScreenshotPath;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public ContributionStatus getStatus() {
        return status;
    }

    public void setStatus(ContributionStatus status) {
        this.status = status;
    }

    public LocalDateTime getContributedAt() {
        return contributedAt;
    }

    public void setContributedAt(LocalDateTime contributedAt) {
        this.contributedAt = contributedAt;
    }

    @Override
    public String toString() {
        return "Contribution{id=" + id + ", amount=" + amount + ", utrNumber='" + utrNumber + "', status=" + status + '}';
    }
}
