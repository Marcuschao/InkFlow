package com.blog.ai.common.support.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIp {

    private ClientIp() {
    }

    public static String resolve(HttpServletRequest req) {
        if (req == null) {
            return "";
        }
        String ip = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            return ip.split(",")[0].trim();
        }
        return req.getRemoteAddr() == null ? "" : req.getRemoteAddr();
    }
}
