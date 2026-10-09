package com.crowdfund.security;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.Role;
import com.crowdfund.entity.User;
import com.crowdfund.exception.BusinessRuleException;
import com.crowdfund.repository.CampaignRepository;
import com.crowdfund.repository.UserRepository;
import com.crowdfund.service.ActivityLogService;
import com.crowdfund.service.CampaignService;
import com.crowdfund.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuthorizationTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private CampaignService campaignService;

    @InjectMocks
    private UserService userService;

    private User creator1;
    private User creator2;
    private User regularUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        creator1 = new User("Creator One", "creator1@test.com", "hash", Role.CREATOR);
        creator1.setId(101L);

        creator2 = new User("Creator Two", "creator2@test.com", "hash", Role.CREATOR);
        creator2.setId(102L);

        regularUser = new User("Contributor", "user@test.com", "hash", Role.CONTRIBUTOR);
        regularUser.setId(103L);

        adminUser = new User("Admin", "admin@test.com", "hash", Role.ADMIN);
        adminUser.setId(999L);
    }

    @Test
    @DisplayName("should block regular contributor from creating campaign")
    void shouldBlockContributorFromCreatingCampaign() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                campaignService.createCampaign("Title", "Description of campaign",
                        BigDecimal.valueOf(1000), LocalDate.now().plusDays(30),
                        "Tech", regularUser)
        );
        assertTrue(ex.getMessage().contains("Only creators can create campaigns"));
    }

    @Test
    @DisplayName("should block non-admin from approving campaign")
    void shouldBlockNonAdminFromApproving() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                campaignService.approveCampaign(1L, creator1)
        );
        assertTrue(ex.getMessage().contains("Only administrators can approve campaigns"));
    }

    @Test
    @DisplayName("should block non-admin from rejecting campaign")
    void shouldBlockNonAdminFromRejecting() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                campaignService.rejectCampaign(1L, "reason", regularUser)
        );
        assertTrue(ex.getMessage().contains("Only administrators can reject campaigns"));
    }

    @Test
    @DisplayName("should prevent creator from modifying another creator's campaign")
    void shouldPreventCrossCreatorModification() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setCreator(creator1);
        campaign.setTitle("Original Title");

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                campaignService.updateCampaign(1L, "Hacked Title", "New description here", "Tech", creator2)
        );
        assertTrue(ex.getMessage().contains("only edit your own campaigns"));
    }

    @Test
    @DisplayName("should block self-registration as ADMIN role")
    void shouldBlockAdminSelfRegistration() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                userService.register("Admin Impersonator", "admin@hacker.com", "secretPass", Role.ADMIN)
        );
        assertTrue(ex.getMessage().contains("Self-registration as ADMIN is not permitted"));
    }

    @Test
    @DisplayName("should block non-admin from creating users via admin endpoint")
    void shouldBlockNonAdminUserCreation() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                userService.createUser("Target", "target@test.com", "pass", Role.CONTRIBUTOR, regularUser)
        );
        assertTrue(ex.getMessage().contains("Only administrators can create users"));
    }
}
