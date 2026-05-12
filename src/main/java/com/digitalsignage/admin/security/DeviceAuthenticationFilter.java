package com.digitalsignage.admin.security;

import com.digitalsignage.admin.common.enums.ActivationStatus;
import com.digitalsignage.admin.entity.Screen;
import com.digitalsignage.admin.screen.repository.ScreenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnBean(ScreenRepository.class)
@RequiredArgsConstructor
public class DeviceAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ScreenRepository screenRepository;

    @Value("${app.api.device-prefix}")
    private String devicePrefix;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        if (!path.startsWith(devicePrefix)) {
            return true;
        }
        String activatePath = devicePrefix + "/activate";
        return path.equals(activatePath);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()
                && existing.getPrincipal() instanceof AdminPrincipal) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String raw = header.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(raw)) {
            filterChain.doFilter(request, response);
            return;
        }

        screenRepository.findByDeviceToken(raw).ifPresent(screen -> {
            if (screen.getActivationStatus() != ActivationStatus.ACTIVATED) {
                return;
            }
            DevicePrincipal principal = DevicePrincipal.builder()
                    .screenId(screen.getId())
                    .organizationId(screen.getOrganization().getId())
                    .deviceCode(screen.getDeviceCode())
                    .build();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });

        filterChain.doFilter(request, response);
    }
}
