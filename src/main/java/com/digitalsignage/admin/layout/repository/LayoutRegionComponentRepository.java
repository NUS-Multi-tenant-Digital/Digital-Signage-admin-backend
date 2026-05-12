package com.digitalsignage.admin.layout.repository;

import com.digitalsignage.admin.entity.LayoutRegionComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LayoutRegionComponentRepository extends JpaRepository<LayoutRegionComponent, Long> {

    @Query("SELECT c FROM LayoutRegionComponent c WHERE c.region.id IN :regionIds ORDER BY c.sortOrder ASC, c.id ASC")
    List<LayoutRegionComponent> findByRegion_IdIn(@Param("regionIds") Collection<Long> regionIds);
}
