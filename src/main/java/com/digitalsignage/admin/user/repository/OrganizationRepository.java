package com.digitalsignage.admin.user.repository;

import com.digitalsignage.admin.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByCode(String code);

    Optional<Organization> findByCode(String code);
}
