package com.mall.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端IP获取工具类。
 * 处理反向代理场景下的真实IP识别，支持常见代理头：
 * X-Forwarded-For、X-Real-IP、Proxy-Client-IP、WL-Proxy-Client-IP。
 */
public class IpUtil {

    private IpUtil() {
    }

    /**
     * 获取客户端真实IP地址。
     * 优先从 X-Forwarded-For 中取第一个IP，其次检查 X-Real-IP 等代理头，
     * 所有代理头都不存在时回退到 request.getRemoteAddr()。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 1. X-Forwarded-For：反向代理链中最常用的头，可能包含多个IP（逗号分隔）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 取第一个IP作为客户端真实IP
            int commaIndex = ip.indexOf(',');
            if (commaIndex != -1) {
                return ip.substring(0, commaIndex).trim();
            }
            return ip.trim();
        }

        // 2. X-Real-IP：nginx 代理常用的单IP头
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // 3. Proxy-Client-IP：Apache 代理常用
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // 4. WL-Proxy-Client-IP：WebLogic 代理常用
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // 5. 回退到 remoteAddr
        return request.getRemoteAddr();
    }
}