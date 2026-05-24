package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.crypto.digest.DigestUtil;
import org.junit.jupiter.api.Test;

class PasswordHashServiceTest {

    private final PasswordHashService service = new PasswordHashService();

    @Test
    void hashesNewPasswordsWithBcryptPrefix() {
        String hash = service.hash("secret123");

        assertTrue(hash.startsWith("{bcrypt}"));
        assertFalse(hash.contains("secret123"));
        assertTrue(service.matches("secret123", hash));
        assertFalse(service.matches("wrong", hash));
    }

    @Test
    void acceptsLegacyMd5FormatsAndMarksThemForUpgrade() {
        String bareMd5 = DigestUtil.md5Hex("secret123");
        String taggedMd5 = "{MD5}" + bareMd5;

        assertTrue(service.matches("secret123", bareMd5));
        assertTrue(service.matches("secret123", taggedMd5));
        assertTrue(service.needsUpgrade(bareMd5));
        assertTrue(service.needsUpgrade(taggedMd5));
    }
}
