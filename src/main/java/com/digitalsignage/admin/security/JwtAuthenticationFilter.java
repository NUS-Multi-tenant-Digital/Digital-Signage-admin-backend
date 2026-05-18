package com.digitalsignage.admin.security;

import com.digitalsignage.admin.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();

    private final JwtService jwtService;

    @Value("${app.api.device-prefix}")
    private String devicePrefix;

    /**
     * Device routes use {@link DeviceAuthenticationFilter}; never interpret Bearer as JWT here.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = PATH_HELPER.getPathWithinApplication(request);
        return path.equals(devicePrefix) || path.startsWith(devicePrefix + "/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String raw = header.substring(BEARER_PREFIX.length()).trim();
            if (StringUtils.hasText(raw) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    AdminPrincipal principal = jwtService.parseAccessToken(raw);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (BusinessException ex) {
                    log.debug("Access token rejected: {}", ex.getMessage());
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
