package com.digitalsignage.admin.schedule.repository;

import com.digitalsignage.admin.entity.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByOrganization_Id(Long organizationId);

    @EntityGraph(attributePaths = {"layout", "playlist", "screen", "screenGroup"})
    List<Schedule> findByOrganization_IdOrderByUpdatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = {"layout", "playlist", "screen", "screenGroup"})
    Optional<Schedule> findByIdAndOrganization_Id(Long id, Long organizationId);

    List<Schedule> findByLayout_Id(Long layoutId);

    List<Schedule> findByPlaylist_Id(Long playlistId);

    @Query("SELECT DISTINCT s FROM Schedule s JOIN FETCH s.organization "
            + "LEFT JOIN FETCH s.screen LEFT JOIN FETCH s.screenGroup "
            + "WHERE s.layout.id = :layoutId")
    List<Schedule> fetchByLayout_IdWithOrganization(@Param("layoutId") Long layoutId);

    @Query("SELECT DISTINCT s FROM Schedule s JOIN FETCH s.layout WHERE s.playlist.id = :playlistId")
    List<Schedule> fetchByPlaylist_IdWithLayout(@Param("playlistId") Long playlistId);

    @Query("SELECT DISTINCT s FROM Schedule s JOIN FETCH s.layout JOIN FETCH s.playlist "
            + "LEFT JOIN FETCH s.screen LEFT JOIN FETCH s.screenGroup "
            + "WHERE s.organization.id = :orgId")
    List<Schedule> fetchAllForOrganization(@Param("orgId") Long orgId);

    boolean existsByLayout_Id(Long layoutId);

    boolean existsByPlaylist_Id(Long playlistId);

    boolean existsByScreen_Id(Long screenId);

    boolean existsByScreenGroup_Id(Long screenGroupId);
}
