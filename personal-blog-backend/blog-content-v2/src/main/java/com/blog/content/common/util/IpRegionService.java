package com.blog.content.common.util;

import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.ConfigBuilder;
import org.lionsoul.ip2region.service.Ip2Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class IpRegionService {

    private static final Logger log = LoggerFactory.getLogger(IpRegionService.class);
    private static final String V4_XDB = "ip2region/ip2region_v4.xdb";
    private static final String V6_XDB = "ip2region/ip2region_v6.xdb";

    private volatile Ip2Region ip2Region;
    private volatile boolean initialized;

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            try {
                Config v4 = loadConfig(V4_XDB, true);
                Config v6 = loadConfig(V6_XDB, false);
                if (v4 == null && v6 == null) {
                    log.warn("ip2region xdb not found, put {} and/or {} under classpath", V4_XDB, V6_XDB);
                } else {
                    ip2Region = Ip2Region.create(v4, v6);
                    log.info("ip2region loaded: v4={}, v6={}", v4 != null, v6 != null);
                }
            } catch (Exception e) {
                log.warn("Failed to load ip2region: {}", e.getMessage());
            }
            initialized = true;
        }
    }

    @PreDestroy
    void destroy() {
        if (ip2Region != null) {
            try {
                ip2Region.close();
            } catch (Exception ignored) {
            }
        }
    }

    private Config loadConfig(String classpath, boolean v4) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpath);
        if (!resource.exists()) {
            log.warn("missing {}", classpath);
            return null;
        }
        byte[] buf;
        try (InputStream in = resource.getInputStream()) {
            buf = in.readAllBytes();
        }
        ConfigBuilder builder = Config.custom()
                .setCachePolicy(Config.BufferCache)
                .setXdbInputStream(new ByteArrayInputStream(buf));
        return v4 ? builder.asV4() : builder.asV6();
    }

    public String resolve(String ip) {
        if (!StringUtils.hasText(ip) || isInternal(ip.trim())) {
            return "未知";
        }
        ensureInitialized();
        if (ip2Region == null) {
            return "未知";
        }
        try {
            String raw = ip2Region.search(ip.trim());
            return format(raw);
        } catch (Exception e) {
            return "未知";
        }
    }

    static boolean isInternal(String ip) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        if (ip.contains(":")) {
            String lower = ip.toLowerCase();
            return lower.startsWith("fe80:")
                    || lower.startsWith("fc")
                    || lower.startsWith("fd")
                    || lower.startsWith("::ffff:127.")
                    || lower.startsWith("::ffff:10.")
                    || lower.startsWith("::ffff:192.168.");
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ip.startsWith("169.254.");
    }

    static String format(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "未知";
        }
        String[] parts = raw.split("\\|");
        if (parts.length < 5) {
            return "未知";
        }
        String province = trimRegion(parts[2]);
        String city = trimRegion(parts[3]);
        if (!StringUtils.hasText(province) && !StringUtils.hasText(city)) {
            return "未知";
        }
        if ("0".equals(province) || "内网IP".equals(province)) {
            return "未知";
        }
        if (!StringUtils.hasText(city) || "0".equals(city)) {
            return province;
        }
        if (province.equals(city)) {
            return province;
        }
        return province + " " + city;
    }

    private static String trimRegion(String s) {
        if (!StringUtils.hasText(s) || "0".equals(s)) {
            return "";
        }
        return s.replace("省", "").replace("市", "").trim();
    }
}
