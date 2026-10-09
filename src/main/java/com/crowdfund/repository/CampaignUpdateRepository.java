package com.crowdfund.repository;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.CampaignUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignUpdateRepository extends JpaRepository<CampaignUpdate, Long> {

    List<CampaignUpdate> findByCampaignOrderByPostedAtDesc(Campaign campaign);

    Page<CampaignUpdate> findByCampaign(Campaign campaign, Pageable pageable);

    List<CampaignUpdate> findByCampaignInOrderByPostedAtDesc(List<Campaign> campaigns);
}
