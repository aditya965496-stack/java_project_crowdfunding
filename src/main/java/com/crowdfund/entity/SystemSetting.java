package com.crowdfund.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * A platform-wide configuration setting stored as a key-value pair.
 * <p>
 * Examples: {@code platform.name}, {@code min.contribution.amount},
 * {@code max.campaign.duration.days}, {@code platform.fee.percentage}.
 * </p>
 */
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String settingKey;

    @Size(max = 500)
    @Column(length = 500)
    private String settingValue;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public SystemSetting() {
    }

    public SystemSetting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
