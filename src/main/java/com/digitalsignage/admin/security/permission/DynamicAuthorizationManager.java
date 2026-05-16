package com.digitalsignage.admin.security.permission;

import com.digitalsignage.admin.security.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Admin API routes: permissions from {@code api_permission} / {@code role_api_permission}.
 */
@Component
@RequiredArgsConstructor
public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final ApiPermissionService apiPermissionService;

    @Value("${app.api.admin-prefix}")
    private String adminPrefix;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String path = RequestPathUtils.resolveRequestPath(request);
        Authentication authentication = authenticationSupplier.get();

        if (path.startsWith(adminPrefix)) {
            return new AuthorizationDecision(checkAdmin(authentication, request));
        }
        return new AuthorizationDecision(authentication != null && authentication.isAuthenticated());
    }

    private boolean checkAdmin(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof AdminPrincipal admin)) {
            return false;
        }
        return apiPermissionService.hasAdminAccess(admin.getRole(), request);
    }
}
