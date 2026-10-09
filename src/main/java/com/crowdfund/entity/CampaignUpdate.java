package com.crowdfund.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * A progress update posted by a Campaign Creator to keep contributors informed.
 */
@Entity
@Table(name = "campaign_updates")
public class CampaignUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "posted_by", nullable = false)
    private User postedBy;

    @NotBlank(message = "Content is required")
    @Size(max = 3000)
    @Column(nullable = false, length = 3000)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime postedAt;

    @PrePersist
    protected void onCreate() {
        this.postedAt = LocalDateTime.now();
    }

    public CampaignUpdate() {
    }

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

    public User getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(User postedBy) {
        this.postedBy = postedBy;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }
}
