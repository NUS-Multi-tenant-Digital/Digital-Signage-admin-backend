package com.digitalsignage.admin.security.permission;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestPathUtils {

    private RequestPathUtils() {
    }

    public static String resolveRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri == null || uri.isEmpty() ? "/" : uri;
    }
}
