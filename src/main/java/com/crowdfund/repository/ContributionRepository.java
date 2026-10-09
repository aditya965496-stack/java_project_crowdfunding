package com.crowdfund.repository;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.Contribution;
import com.crowdfund.entity.ContributionStatus;
import com.crowdfund.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data access for {@link Contribution} entities.
 */
@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    Page<Contribution> findByContributor(User contributor, Pageable pageable);

    List<Contribution> findByContributor(User contributor);

    List<Contribution> findByCampaign(Campaign campaign);

    Page<Contribution> findByCampaign(Campaign campaign, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c " +
           "WHERE c.campaign = :campaign AND c.status = 'SUCCESS'")
    BigDecimal sumSuccessfulContributions(@Param("campaign") Campaign campaign);

    @Query("SELECT COUNT(DISTINCT c.contributor) FROM Contribution c " +
           "WHERE c.campaign = :campaign AND c.status = 'SUCCESS'")
    long countDistinctContributors(@Param("campaign") Campaign campaign);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c " +
           "WHERE c.contributor = :contributor AND c.status = 'SUCCESS'")
    BigDecimal sumByContributor(@Param("contributor") User contributor);

    @Query("SELECT DISTINCT c.campaign FROM Contribution c " +
           "WHERE c.contributor = :contributor AND c.status = 'SUCCESS'")
    List<Campaign> findCampaignsByContributor(@Param("contributor") User contributor);

    long countByCampaignAndStatus(Campaign campaign, ContributionStatus status);

    List<Contribution> findByStatusOrderByContributedAtDesc(ContributionStatus status);

    List<Contribution> findAllByOrderByContributedAtDesc();

    long countByStatus(ContributionStatus status);

    java.util.Optional<Contribution> findByUtrNumber(String utrNumber);

    java.util.Optional<Contribution> findByIdempotencyKey(String idempotencyKey);

    java.util.Optional<Contribution> findByTransactionId(String transactionId);
}
