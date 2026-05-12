package com.digitalsignage.admin.screengroup.repository;

import com.digitalsignage.admin.entity.ScreenGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScreenGroupRepository extends JpaRepository<ScreenGroup, Long> {

    List<ScreenGroup> findByOrganization_IdOrderByUpdatedAtDesc(Long organizationId);

    Optional<ScreenGroup> findByIdAndOrganization_Id(Long id, Long organizationId);
}
