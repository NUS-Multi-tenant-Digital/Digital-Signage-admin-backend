package com.digitalsignage.admin.security.permission;

import com.digitalsignage.admin.common.enums.PermissionPrincipalType;
import com.digitalsignage.admin.entity.ApiPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiPermissionRepository extends JpaRepository<ApiPermission, Long> {

    List<ApiPermission> findByPrincipalTypeOrderBySortOrderAscIdAsc(PermissionPrincipalType principalType);
}
