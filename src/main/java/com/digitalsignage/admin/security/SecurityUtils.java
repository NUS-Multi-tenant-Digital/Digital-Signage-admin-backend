package com.digitalsignage.admin.security;

import com.digitalsignage.admin.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AdminPrincipal requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminPrincipal principal) {
            return principal;
        }
        throw new BusinessException(401, "unauthorized");
    }

    public static DevicePrincipal requireDevice() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof DevicePrincipal principal) {
            return principal;
        }
        throw new BusinessException(403, "device authentication required");
    }
}
