package com.college.resourcefinder.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility methods for extracting security-relevant information from HTTP requests.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Extracts the client IP address, honouring common reverse-proxy headers.
     * Falls back to {@link HttpServletRequest#getRemoteAddr()} if no proxy headers present.
     */
    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For may contain comma-separated list; take the first
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
