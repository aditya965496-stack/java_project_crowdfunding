package com.crowdfund.repository;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.CampaignStatus;
import com.crowdfund.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access for {@link Campaign} entities.
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);

    List<Campaign> findByCreator(User creator);

    Page<Campaign> findByCreator(User creator, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status = :status AND c.deadline < :date")
    List<Campaign> findExpiredCampaigns(@Param("status") CampaignStatus status,
                                        @Param("date") LocalDate date);

    @Query("SELECT c FROM Campaign c WHERE c.status IN ('APPROVED', 'ACTIVE') " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Campaign> searchActiveCampaigns(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status IN ('APPROVED', 'ACTIVE')")
    Page<Campaign> findPublicCampaigns(Pageable pageable);

    long countByStatus(CampaignStatus status);

    @Query("SELECT COALESCE(SUM(c.raisedAmount), 0) FROM Campaign c")
    java.math.BigDecimal totalRaisedAcrossAllCampaigns();
}
