package com.digitalsignage.admin.security.permission;

import com.digitalsignage.admin.entity.RoleApiPermission;
import com.digitalsignage.admin.entity.RoleApiPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoleApiPermissionRepository extends JpaRepository<RoleApiPermission, RoleApiPermissionId> {

    @Query("SELECT rap.role, rap.permissionId FROM RoleApiPermission rap")
    List<Object[]> findAllRolePermissionPairs();
}
