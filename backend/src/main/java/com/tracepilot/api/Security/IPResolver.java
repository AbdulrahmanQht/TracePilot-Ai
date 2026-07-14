package com.tracepilot.api.Security;

import jakarta.servlet.http.HttpServletRequest;

public class IPResolver {
    public static String getClientIp(HttpServletRequest request) {
        // Check Cloudflare header first, then standard proxy, then fall back to remote
        // address
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain a comma-separated list of hop IPs. Grab the first
            // one (original client).
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}