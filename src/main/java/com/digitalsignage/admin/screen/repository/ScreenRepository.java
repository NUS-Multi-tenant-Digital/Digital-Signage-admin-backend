package com.digitalsignage.admin.screen.repository;

import com.digitalsignage.admin.common.enums.ScreenStatus;
import com.digitalsignage.admin.entity.Screen;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    @EntityGraph(attributePaths = "screenGroup")
    List<Screen> findByOrganization_IdOrderByUpdatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = "screenGroup")
    Optional<Screen> findByIdAndOrganization_Id(Long id, Long organizationId);

    Optional<Screen> findByDeviceCode(String deviceCode);

    boolean existsByDeviceCode(String deviceCode);

    @EntityGraph(attributePaths = "organization")
    Optional<Screen> findByDeviceToken(String deviceToken);

    List<Screen> findByScreenGroup_Id(Long screenGroupId);

    List<Screen> findByOrganization_Id(Long organizationId);

    boolean existsByScreenGroup_Id(Long screenGroupId);

    @Query("SELECT DISTINCT s FROM Screen s JOIN FETCH s.organization LEFT JOIN FETCH s.screenGroup WHERE s.id = :id")
    Optional<Screen> fetchForResolve(@Param("id") Long id);

    long countByOrganization_Id(Long organizationId);

    long countByOrganization_IdAndStatus(Long organizationId, ScreenStatus status);
}
