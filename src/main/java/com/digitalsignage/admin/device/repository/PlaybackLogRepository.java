package com.digitalsignage.admin.device.repository;

import com.digitalsignage.admin.entity.PlaybackLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PlaybackLogRepository extends JpaRepository<PlaybackLog, Long> {

    Page<PlaybackLog> findByScreen_IdOrderByPlayedAtDesc(Long screenId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM PlaybackLog p JOIN p.screen s WHERE s.organization.id = :orgId "
            + "AND p.playedAt >= :start AND p.playedAt < :end")
    long countPlaysForOrganizationBetween(
            @Param("orgId") Long organizationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
