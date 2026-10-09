package com.crowdfund.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A fundraising campaign created by a user with the {@code CREATOR} role.
 * <p>
 * The {@link #raisedAmount} is always derived from the sum of successful
 * contributions and must never be manually edited. The campaign follows
 * the lifecycle defined by {@link CampaignStatus}.
 * </p>
 */
@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000)
    @Column(nullable = false, length = 5000)
    private String description;

    @NotNull(message = "Goal amount is required")
    @DecimalMin(value = "1.00", message = "Goal must be at least 1.00")
    @DecimalMax(value = "100000000.00", message = "Goal cannot exceed 100,000,000.00")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal goalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal raisedAmount = BigDecimal.ZERO;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    @Column(nullable = false)
    private LocalDate deadline;

    @Size(max = 100)
    @Column(length = 100)
    private String category;

    @Size(max = 500)
    @Column(length = 500)
    private String imageUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.PENDING;

    @Size(max = 1000)
    @Column(length = 1000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contribution> contributions = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignUpdate> updates = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.raisedAmount == null) {
            this.raisedAmount = BigDecimal.ZERO;
        }
    }

    // ── Constructors ──────────────────────────────────────────

    public Campaign() {
    }

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(BigDecimal goalAmount) {
        this.goalAmount = goalAmount;
    }

    public BigDecimal getRaisedAmount() {
        return raisedAmount;
    }

    public void setRaisedAmount(BigDecimal raisedAmount) {
        this.raisedAmount = raisedAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public List<Contribution> getContributions() {
        return contributions;
    }

    public void setContributions(List<Contribution> contributions) {
        this.contributions = contributions;
    }

    public List<CampaignUpdate> getUpdates() {
        return updates;
    }

    public void setUpdates(List<CampaignUpdate> updates) {
        this.updates = updates;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the funding progress as a percentage (0–100).
     */
    public int getProgressPercentage() {
        if (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return raisedAmount.multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    @Override
    public String toString() {
        return "Campaign{id=" + id + ", title='" + title + "', status=" + status + '}';
    }
}
