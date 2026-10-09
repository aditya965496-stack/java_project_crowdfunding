package com.crowdfund.repository;

import com.crowdfund.entity.Campaign;
import com.crowdfund.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByCampaignOrderBySentAtDesc(Campaign campaign);

    List<Message> findByCampaignInOrderBySentAtDesc(List<Campaign> campaigns);
}
