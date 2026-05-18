package com.digitalsignage.admin.device.repository;

import com.digitalsignage.admin.entity.DeviceEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DeviceEventLogRepository extends JpaRepository<DeviceEventLog, Long> {

    Page<DeviceEventLog> findByScreen_IdOrderByEventTimestampDescCreatedAtDesc(Long screenId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM DeviceEventLog e JOIN e.screen s WHERE s.organization.id = :orgId "
            + "AND e.eventLevel IN ('WARN', 'ERROR') AND e.createdAt >= :since")
    long countAlertsForOrganizationSince(@Param("orgId") Long orgId, @Param("since") LocalDateTime since);
}
