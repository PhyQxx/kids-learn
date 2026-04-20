package com.kidslearn.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtil {

    private static final String SECRET = "kidslearn-secret-key-for-jwt-token-generation-must-be-at-least-256-bits";

    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token
     */
    public static String generateToken(Long userId, String userType, long expireSeconds) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("userType", userType))
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析Token
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token获取用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从Token获取用户类型
     */
    public static String getUserType(String token) {
        Claims claims = parseToken(token);
        return claims.get("userType", String.class);
    }

    /**
     * 验证Token是否过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
