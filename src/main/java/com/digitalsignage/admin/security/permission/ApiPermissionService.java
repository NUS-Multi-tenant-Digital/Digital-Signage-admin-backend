package com.digitalsignage.admin.security.permission;

import com.digitalsignage.admin.common.enums.PermissionPrincipalType;
import com.digitalsignage.admin.common.enums.UserRole;
import com.digitalsignage.admin.entity.ApiPermission;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApiPermissionService {

    private final ApiPermissionRepository apiPermissionRepository;
    private final RoleApiPermissionRepository roleApiPermissionRepository;

    private List<CachedApiPermission> adminPermissions = List.of();

    @PostConstruct
    @Transactional(readOnly = true)
    public void loadCache() {
        reloadCache();
    }

    @Transactional(readOnly = true)
    public void reloadCache() {
        Map<Long, Set<String>> rolesByPermissionId = new HashMap<>();
        for (Object[] row : roleApiPermissionRepository.findAllRolePermissionPairs()) {
            String role = (String) row[0];
            Long permissionId = (Long) row[1];
            rolesByPermissionId.computeIfAbsent(permissionId, ignored -> new HashSet<>()).add(role);
        }

        adminPermissions = buildCached(
                apiPermissionRepository.findByPrincipalTypeOrderBySortOrderAscIdAsc(PermissionPrincipalType.ADMIN),
                rolesByPermissionId);
    }

    public boolean hasAdminAccess(UserRole role, HttpServletRequest request) {
        return matches(adminPermissions, role.name(), request);
    }

    private static boolean matches(List<CachedApiPermission> permissions, String roleName, HttpServletRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String path = RequestPathUtils.resolveRequestPath(request);
        for (CachedApiPermission permission : permissions) {
            if (permission.matches(method, path)) {
                return permission.allows(roleName);
            }
        }
        return false;
    }

    private static List<CachedApiPermission> buildCached(
            List<ApiPermission> permissions,
            Map<Long, Set<String>> rolesByPermissionId) {
        List<CachedApiPermission> cached = new ArrayList<>(permissions.size());
        for (ApiPermission permission : permissions) {
            Set<String> roles = rolesByPermissionId.getOrDefault(permission.getId(), Set.of());
            cached.add(new CachedApiPermission(permission, roles));
        }
        return List.copyOf(cached);
    }
}
