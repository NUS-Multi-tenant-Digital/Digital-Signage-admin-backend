package com.digitalsignage.admin.security.permission;

import com.digitalsignage.admin.common.enums.PermissionAccessMode;
import com.digitalsignage.admin.entity.ApiPermission;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.Set;

final class CachedApiPermission {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiPermission permission;
    private final Set<String> roles;

    CachedApiPermission(ApiPermission permission, Set<String> roles) {
        this.permission = permission;
        this.roles = Set.copyOf(roles);
    }

    boolean matches(HttpMethod method, String path) {
        if (!permission.getHttpMethod().equalsIgnoreCase(method.name())) {
            return false;
        }
        return PATH_MATCHER.match(permission.getPathPattern(), path);
    }

    boolean allows(String roleName) {
        return switch (permission.getAccessMode()) {
            case AUTHENTICATED -> true;
            case ROLE_SCOPED -> roles.contains(roleName);
        };
    }

    PermissionAccessMode accessMode() {
        return permission.getAccessMode();
    }
}
