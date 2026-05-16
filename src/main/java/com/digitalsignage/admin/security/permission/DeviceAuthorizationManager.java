package com.digitalsignage.admin.security.permission;

import com.digitalsignage.admin.security.DevicePrincipal;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Device routes: valid {@link DevicePrincipal} only (no DB permission matrix).
 */
@Component
public class DeviceAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        boolean allowed = authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof DevicePrincipal;
        return new AuthorizationDecision(allowed);
    }
}
