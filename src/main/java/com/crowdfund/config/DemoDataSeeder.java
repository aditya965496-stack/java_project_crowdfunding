package com.crowdfund.config;

import com.crowdfund.entity.*;
import com.crowdfund.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Demo Data Seeder — strictly disabled by default in production.
 * Only runs if the 'demo' Spring profile is active OR 'crowdfund.seed.demo-data=true'.
 */
@Configuration
@Profile("demo")
@ConditionalOnProperty(name = "crowdfund.seed.demo-data", havingValue = "true", matchIfMissing = false)
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    @Bean
    @Order(2)
    CommandLineRunner initDemoData(UserRepository userRepo,
                                  CampaignRepository campaignRepo,
                                  ContributionRepository contribRepo,
                                  ActivityLogRepository logRepo,
                                  CampaignUpdateRepository updateRepo,
                                  PasswordEncoder encoder) {
        return args -> {
            // Only seed demo data if no campaigns exist
            if (campaignRepo.count() > 0) {
                return;
            }
            log.info("Demo mode enabled: Seeding database with demo campaigns and users...");

            // ── Creators ─────────────────────────────────────────────
            User creator1 = new User("Rahul Sharma", "rahul@example.com",
                    encoder.encode("password"), Role.CREATOR);
            creator1 = userRepo.save(creator1);

            User creator2 = new User("Priya Patel", "priya@example.com",
                    encoder.encode("password"), Role.CREATOR);
            creator2 = userRepo.save(creator2);

            // ── Contributors ─────────────────────────────────────────
            User contrib1 = new User("Amit Kumar", "amit@example.com",
                    encoder.encode("password"), Role.CONTRIBUTOR);
            contrib1 = userRepo.save(contrib1);

            User contrib2 = new User("Sneha Reddy", "sneha@example.com",
                    encoder.encode("password"), Role.CONTRIBUTOR);
            contrib2 = userRepo.save(contrib2);

            // ── Campaigns ────────────────────────────────────────────
            Campaign c1 = new Campaign();
            c1.setTitle("Solar Panels for Rural Schools");
            c1.setDescription("Help us install solar panels in 10 rural schools across Rajasthan.");
            c1.setGoalAmount(new BigDecimal("500000.00"));
            c1.setDeadline(LocalDate.now().plusMonths(3));
            c1.setCategory("Education");
            c1.setStatus(CampaignStatus.ACTIVE);
            c1.setCreator(creator1);
            c1 = campaignRepo.save(c1);

            Campaign c2 = new Campaign();
            c2.setTitle("Open-Source Water Purifier");
            c2.setDescription("Low-cost, open-source water purifier design under Creative Commons.");
            c2.setGoalAmount(new BigDecimal("250000.00"));
            c2.setDeadline(LocalDate.now().plusMonths(2));
            c2.setCategory("Technology");
            c2.setStatus(CampaignStatus.ACTIVE);
            c2.setCreator(creator1);
            c2 = campaignRepo.save(c2);

            // ── Contributions ────────────────────────────────────────
            saveContrib(contribRepo, c1, contrib1, "5000.00", "UPI", "DEMO-TXN-001");
            saveContrib(contribRepo, c1, contrib2, "10000.00", "Credit Card", "DEMO-TXN-002");
            saveContrib(contribRepo, c2, contrib1, "15000.00", "UPI", "DEMO-TXN-003");

            // Recalculate
            c1.setRaisedAmount(contribRepo.sumSuccessfulContributions(c1));
            campaignRepo.save(c1);
            c2.setRaisedAmount(contribRepo.sumSuccessfulContributions(c2));
            campaignRepo.save(c2);

            log.info("Demo data seeded.");
        };
    }

    private void saveContrib(ContributionRepository repo, Campaign campaign,
                             User contributor, String amount, String method, String txId) {
        Contribution c = new Contribution();
        c.setCampaign(campaign);
        c.setContributor(contributor);
        c.setAmount(new BigDecimal(amount));
        c.setPaymentMethod(method);
        c.setUpiId("adityalive49@oksbi");
        c.setUtrNumber("UTR-" + txId);
        c.setTransactionId(txId);
        c.setIdempotencyKey("IDEM-" + txId);
        c.setStatus(ContributionStatus.SUCCESS);
        repo.save(c);
    }
}
