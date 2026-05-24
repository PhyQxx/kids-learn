package com.kidslearn.api.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {

    private static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final String MD5_PREFIX = "{MD5}";
    private static final String MD5_HEX_PATTERN = "^[a-fA-F0-9]{32}$";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return BCRYPT_PREFIX + encoder.encode(rawPassword == null ? "" : rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith(BCRYPT_PREFIX)) {
            return encoder.matches(rawPassword, storedPassword.substring(BCRYPT_PREFIX.length()));
        }

        if (isRawBcrypt(storedPassword)) {
            return encoder.matches(rawPassword, storedPassword);
        }

        if (storedPassword.startsWith(MD5_PREFIX)) {
            return DigestUtil.md5Hex(rawPassword).equalsIgnoreCase(storedPassword.substring(MD5_PREFIX.length()));
        }

        if (storedPassword.matches(MD5_HEX_PATTERN)) {
            return DigestUtil.md5Hex(rawPassword).equalsIgnoreCase(storedPassword);
        }

        return false;
    }

    public boolean needsUpgrade(String storedPassword) {
        return storedPassword == null || !storedPassword.startsWith(BCRYPT_PREFIX);
    }

    private boolean isRawBcrypt(String storedPassword) {
        return storedPassword.startsWith("$2a$")
            || storedPassword.startsWith("$2b$")
            || storedPassword.startsWith("$2y$");
    }
}
