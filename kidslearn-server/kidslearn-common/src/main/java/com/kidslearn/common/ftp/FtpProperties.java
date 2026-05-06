package com.kidslearn.common.ftp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ftp")
public class FtpProperties {

    private String host;
    private Integer port = 21;
    private String username;
    private String password;
    private String path = "/ftp";
    private String siteUrl;

    public boolean isConfigured() {
        return hasText(host) && port != null && hasText(username) && hasText(password)
            && hasText(path) && hasText(siteUrl);
    }

    public String normalizedBasePath() {
        if (!hasText(path)) {
            return "";
        }
        String normalized = path.replace("\\", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public String normalizedSiteUrl() {
        if (!hasText(siteUrl)) {
            return "";
        }
        String normalized = siteUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
