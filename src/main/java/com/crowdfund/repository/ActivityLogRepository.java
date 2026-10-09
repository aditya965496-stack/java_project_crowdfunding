package com.crowdfund.repository;

import com.crowdfund.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);

    List<ActivityLog> findTop50ByOrderByTimestampDesc();
}
