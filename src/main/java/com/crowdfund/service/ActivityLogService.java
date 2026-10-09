package com.crowdfund.service;

import com.crowdfund.entity.ActivityLog;
import com.crowdfund.entity.User;
import com.crowdfund.repository.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists activity events for the admin's real-time monitoring feed.
 */
@Service
@Transactional
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * Writes an activity entry. This is called from other services
     * whenever a noteworthy action occurs.
     */
    public ActivityLog log(String type, String description, User actor) {
        ActivityLog entry = new ActivityLog(type, description, actor);
        return activityLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentActivity() {
        return activityLogRepository.findTop50ByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivity(Pageable pageable) {
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
