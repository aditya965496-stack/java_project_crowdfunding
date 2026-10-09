package com.crowdfund.service;

import com.crowdfund.entity.SystemSetting;
import com.crowdfund.repository.SystemSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository settingRepository;

    @InjectMocks
    private SystemSettingService systemSettingService;

    @Test
    @DisplayName("should return configured minimum contribution amount")
    void shouldReturnMinContribution() {
        SystemSetting setting = new SystemSetting("min.contribution.amount", "10.00");
        when(settingRepository.findBySettingKey("min.contribution.amount"))
                .thenReturn(Optional.of(setting));

        BigDecimal result = systemSettingService.getMinContributionAmount();

        assertEquals(new BigDecimal("10.00"), result);
    }

    @Test
    @DisplayName("should fall back to default when setting is missing")
    void shouldFallBackToDefault() {
        when(settingRepository.findBySettingKey("min.contribution.amount"))
                .thenReturn(Optional.empty());

        BigDecimal result = systemSettingService.getMinContributionAmount();

        assertEquals(new BigDecimal("1.00"), result);
    }

    @Test
    @DisplayName("should create new setting if key does not exist")
    void shouldCreateNewSetting() {
        when(settingRepository.findBySettingKey("new.key"))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any(SystemSetting.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SystemSetting result = systemSettingService.saveSetting("new.key", "new.value");

        assertEquals("new.key", result.getSettingKey());
        assertEquals("new.value", result.getSettingValue());
        verify(settingRepository).save(any(SystemSetting.class));
    }

    @Test
    @DisplayName("should update existing setting value")
    void shouldUpdateExisting() {
        SystemSetting existing = new SystemSetting("platform.name", "OldName");
        when(settingRepository.findBySettingKey("platform.name"))
                .thenReturn(Optional.of(existing));
        when(settingRepository.save(any(SystemSetting.class)))
                .thenReturn(existing);

        systemSettingService.saveSetting("platform.name", "NewName");

        assertEquals("NewName", existing.getSettingValue());
    }

    @Test
    @DisplayName("should return platform name")
    void shouldReturnPlatformName() {
        SystemSetting setting = new SystemSetting("platform.name", "TestPlatform");
        when(settingRepository.findBySettingKey("platform.name"))
                .thenReturn(Optional.of(setting));

        String name = systemSettingService.getPlatformName();

        assertEquals("TestPlatform", name);
    }

    @Test
    @DisplayName("should return max campaign duration days")
    void shouldReturnMaxDuration() {
        SystemSetting setting = new SystemSetting("max.campaign.duration.days", "180");
        when(settingRepository.findBySettingKey("max.campaign.duration.days"))
                .thenReturn(Optional.of(setting));

        int days = systemSettingService.getMaxCampaignDurationDays();

        assertEquals(180, days);
    }
}
