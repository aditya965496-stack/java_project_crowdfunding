package com.crowdfund.service;

import com.crowdfund.entity.SystemSetting;
import com.crowdfund.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Manages platform-wide configuration settings.
 * <p>
 * Settings are stored as key-value pairs. Defaults are seeded at
 * startup by the data initialiser.
 * </p>
 */
@Service
@Transactional
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;

    public SystemSettingService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> getValue(String key) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue);
    }

    @Transactional(readOnly = true)
    public String getValueOrDefault(String key, String defaultValue) {
        return getValue(key).orElse(defaultValue);
    }

    /**
     * Returns the minimum contribution amount set by the admin.
     * Falls back to 1.00 if not configured.
     */
    @Transactional(readOnly = true)
    public BigDecimal getMinContributionAmount() {
        String val = getValueOrDefault("min.contribution.amount", "1.00");
        return new BigDecimal(val);
    }

    @Transactional(readOnly = true)
    public int getMaxCampaignDurationDays() {
        String val = getValueOrDefault("max.campaign.duration.days", "365");
        return Integer.parseInt(val);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPlatformFeePercentage() {
        String val = getValueOrDefault("platform.fee.percentage", "5.00");
        return new BigDecimal(val);
    }

    @Transactional(readOnly = true)
    public String getPlatformName() {
        return getValueOrDefault("platform.name", "CrowdFund");
    }

    public SystemSetting saveSetting(String key, String value) {
        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElse(new SystemSetting(key, value));
        setting.setSettingValue(value);
        return settingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return settingRepository.findAll();
    }
}
