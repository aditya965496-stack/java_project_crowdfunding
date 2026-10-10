package com.crowdfund.config;

import com.crowdfund.entity.*;
import com.crowdfund.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializes core platform configuration, administrative credentials,
 * applies schema patches for enum constraints, and seeds sample pending UPI payment records.
 */
@Configuration
public class CoreDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(CoreDataInitializer.class);

    @Value("${crowdfund.security.admin.email:admin@crowdfund.com}")
    private String adminEmail;

    @Value("${crowdfund.security.admin.password:Admin@Secure2026!}")
    private String adminPassword;

    @Bean
    @Order(1)
    CommandLineRunner initCoreData(UserRepository userRepo,
                                  CampaignRepository campaignRepo,
                                  ContributionRepository contribRepo,
                                  SystemSettingRepository settingRepo,
                                  ActivityLogRepository logRepo,
                                  PasswordEncoder encoder,
                                  DataSource dataSource,
                                  org.springframework.transaction.support.TransactionTemplate txTemplate) {
        return args -> {
            // ── Patch H2 Enum Check Constraints ───────────────────────
            patchDatabaseSchema(dataSource);

            txTemplate.executeWithoutResult(status -> {
                // ── System Settings ──────────────────────────────────
                ensureSetting(settingRepo, "platform.name", "CrowdFund");
                ensureSetting(settingRepo, "min.contribution.amount", "5.00");
                ensureSetting(settingRepo, "max.campaign.duration.days", "365");
                ensureSetting(settingRepo, "platform.fee.percentage", "5.00");

                // ── Core Initial Administrator ───────────────────────
                userRepo.findByEmail(adminEmail).ifPresentOrElse(admin -> {
                    admin.setPasswordHash(encoder.encode(adminPassword));
                    admin.setActive(true);
                    admin.setRole(Role.ADMIN);
                    userRepo.save(admin);
                    log.info("Synchronized administrator credentials for: {}", adminEmail);
                }, () -> {
                    User admin = new User("System Administrator", adminEmail,
                            encoder.encode(adminPassword), Role.ADMIN);
                    admin = userRepo.save(admin);
                    logRepo.save(new ActivityLog("SYSTEM", "Initialized root administrator account", admin));
                    log.info("Initialized root administrator: {}", adminEmail);
                });

                // ── Sample Pending UPI Payment for Admin Portal verification ──
                try {
                    if (contribRepo.findByIdempotencyKey("IDEM-UPI-DEMO-VERIFY1").isEmpty()) {
                        List<Campaign> campaigns = campaignRepo.findByStatus(CampaignStatus.ACTIVE, org.springframework.data.domain.Pageable.unpaged()).getContent();
                        List<User> contributors = userRepo.findByRole(Role.CONTRIBUTOR);

                        if (!campaigns.isEmpty() && !contributors.isEmpty()) {
                            Campaign campaign = campaigns.get(0);
                            User contributor = contributors.get(0);

                            Contribution pendingContrib = new Contribution();
                            pendingContrib.setCampaign(campaign);
                            pendingContrib.setContributor(contributor);
                            pendingContrib.setAmount(new BigDecimal("1500.00"));
                            pendingContrib.setPaymentMethod("UPI");
                            pendingContrib.setUpiId("adityalive49@oksbi");
                            pendingContrib.setUtrNumber("428791028374");
                            pendingContrib.setPaymentScreenshotPath("/uploads/payments/sample_upi_receipt.png");
                            pendingContrib.setTransactionId("UPI-DEMO-VERIFY1");
                            pendingContrib.setIdempotencyKey("IDEM-UPI-DEMO-VERIFY1");
                            pendingContrib.setStatus(ContributionStatus.PENDING_VERIFICATION);
                            pendingContrib.setContributedAt(LocalDateTime.now().minusMinutes(25));
                            contribRepo.save(pendingContrib);

                            log.info("Seeded sample pending UPI contribution with UTR: 428791028374 for Admin verification");
                        }
                    }
                } catch (Exception e) {
                    log.warn("Sample pending payment init notice: {}", e.getMessage());
                }
            });
        };
    }

    private void patchDatabaseSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Drop check constraints on CONTRIBUTIONS table so new enum values are accepted
            try {
                ResultSet rs = stmt.executeQuery(
                        "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS WHERE UPPER(TABLE_NAME)='CONTRIBUTIONS' AND CONSTRAINT_TYPE='CHECK'"
                );
                List<String> toDrop = new ArrayList<>();
                while (rs.next()) {
                    toDrop.add(rs.getString("CONSTRAINT_NAME"));
                }
                for (String cName : toDrop) {
                    stmt.execute("ALTER TABLE contributions DROP CONSTRAINT " + cName);
                    log.info("Dropped legacy constraint: {}", cName);
                }
            } catch (Exception ignored) {
            }

            try {
                stmt.execute("ALTER TABLE contributions ALTER COLUMN status VARCHAR(50)");
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.warn("Database schema patch note: {}", e.getMessage());
        }
    }

    private void ensureSetting(SystemSettingRepository repo, String key, String defaultValue) {
        if (!repo.existsBySettingKey(key)) {
            repo.save(new SystemSetting(key, defaultValue));
        }
    }
}
