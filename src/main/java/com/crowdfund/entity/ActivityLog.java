package com.crowdfund.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Captures platform activity for the admin's real-time feed.
 * <p>
 * Each log entry records what happened ({@link #type} + {@link #description})
 * and who triggered it ({@link #actor}).
 * </p>
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String type;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog() {
    }

    public ActivityLog(String type, String description, User actor) {
        this.type = type;
        this.description = description;
        this.actor = actor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
