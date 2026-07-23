package com.kidslearn.api.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.kidslearn.common.exception.BusinessException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/** Purpose-bound, one-time SMS verification codes with distributed rate limits. */
@Service
public class SmsVerificationService {

    public enum Purpose { REGISTER, PASSWORD_RESET, PHONE_CHANGE }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('EXISTS', KEYS[2]) == 1 then return -3 end
        local stored = redis.call('HGET', KEYS[1], 'hash')
        if not stored then return -2 end
        if stored == ARGV[1] then
          redis.call('DEL', KEYS[1])
          redis.call('DEL', KEYS[2])
          return 1
        end
        local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
        if attempts >= tonumber(ARGV[2]) then
          redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])
          redis.call('DEL', KEYS[1])
          return -3
        end
        return -1
        """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;
    private final String pepper;
    private final String gatewayUrl;
    private final String gatewayToken;
    private final String devFixedCode;
    private final int expiresMinutes;
    private final int resendSeconds;
    private final int dailyLimit;
    private final int ipHourlyLimit;
    private final int maxAttempts;
    private final int lockMinutes;

    public SmsVerificationService(
            StringRedisTemplate redisTemplate,
            Environment environment,
            @Value("${kidslearn.sms.pepper:dev-only-change-me}") String pepper,
            @Value("${kidslearn.sms.gateway-url:}") String gatewayUrl,
            @Value("${kidslearn.sms.gateway-token:}") String gatewayToken,
            @Value("${kidslearn.sms.dev-fixed-code:246810}") String devFixedCode,
            @Value("${kidslearn.sms.expires-minutes:5}") int expiresMinutes,
            @Value("${kidslearn.sms.resend-seconds:60}") int resendSeconds,
            @Value("${kidslearn.sms.daily-limit:10}") int dailyLimit,
            @Value("${kidslearn.sms.ip-hourly-limit:20}") int ipHourlyLimit,
            @Value("${kidslearn.sms.max-attempts:5}") int maxAttempts,
            @Value("${kidslearn.sms.lock-minutes:30}") int lockMinutes) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.pepper = pepper;
        this.gatewayUrl = gatewayUrl;
        this.gatewayToken = gatewayToken;
        this.devFixedCode = devFixedCode;
        this.expiresMinutes = expiresMinutes;
        this.resendSeconds = resendSeconds;
        this.dailyLimit = dailyLimit;
        this.ipHourlyLimit = ipHourlyLimit;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    public Map<String, Object> send(String rawPhone, Purpose purpose, String clientIp) {
        String phone = normalizePhone(rawPhone);
        String cooldownKey = "sms:send-cooldown:" + phone;
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(cooldownKey, "1", resendSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }
        try {
            enforceCounter("sms:send-daily:" + LocalDate.now() + ":" + phone,
                dailyLimit, 2, TimeUnit.DAYS, "今日验证码发送次数已达上限");
            enforceCounter("sms:send-ip-hour:" + currentHour() + ":" + safeIp(clientIp),
                ipHourlyLimit, 2, TimeUnit.HOURS, "当前网络请求过于频繁");

            String code = isProduction() ? randomCode() : devFixedCode;
            deliver(phone, code, purpose);
            String requestId = IdUtil.fastSimpleUUID();
            String key = codeKey(purpose, phone);
            redisTemplate.opsForHash().putAll(key, Map.of(
                "hash", hash(code),
                "attempts", "0",
                "issuedAt", LocalDateTime.now().toString(),
                "requestId", requestId
            ));
            redisTemplate.expire(key, expiresMinutes, TimeUnit.MINUTES);
            return Map.of("requestId", requestId, "expiresIn", expiresMinutes * 60);
        } catch (RuntimeException error) {
            redisTemplate.delete(cooldownKey);
            throw error;
        }
    }

    public void verifyAndConsume(String rawPhone, Purpose purpose, String code) {
        String phone = normalizePhone(rawPhone);
        if (code == null || !code.matches("^\\d{6}$")) {
            throw new BusinessException("验证码格式不正确");
        }
        Long result = redisTemplate.execute(
            VERIFY_SCRIPT,
            List.of(codeKey(purpose, phone), lockKey(purpose, phone)),
            hash(code), String.valueOf(maxAttempts), String.valueOf(lockMinutes * 60L)
        );
        if (Long.valueOf(1).equals(result)) return;
        if (Long.valueOf(-3).equals(result)) {
            throw new BusinessException("验证码尝试次数过多，请稍后再试");
        }
        if (Long.valueOf(-2).equals(result)) {
            throw new BusinessException("验证码已过期或已使用");
        }
        throw new BusinessException("验证码错误");
    }

    public static String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("[\\s-]", "");
        if (!normalized.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        return normalized;
    }

    private void deliver(String phone, String code, Purpose purpose) {
        if (!isProduction()) return;
        if (gatewayUrl == null || gatewayUrl.isBlank()
                || pepper == null || pepper.isBlank() || "dev-only-change-me".equals(pepper)) {
            throw new BusinessException("短信服务暂不可用，请稍后重试");
        }
        var response = HttpRequest.post(gatewayUrl)
            .header("Authorization", gatewayToken == null || gatewayToken.isBlank() ? "" : "Bearer " + gatewayToken)
            .body(JSONUtil.toJsonStr(Map.of("phone", phone, "code", code, "purpose", purpose.name())))
            .timeout(5000)
            .execute();
        if (!response.isOk()) {
            throw new BusinessException("短信发送失败，请稍后重试");
        }
    }

    private void enforceCounter(String key, int limit, long ttl, TimeUnit unit, String message) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) redisTemplate.expire(key, ttl, unit);
        if (count != null && count > limit) throw new BusinessException(message);
    }

    private String hash(String code) { return DigestUtil.sha256Hex(code + ":" + pepper); }
    private String codeKey(Purpose purpose, String phone) { return "sms:code:" + purpose + ":" + phone; }
    private String lockKey(Purpose purpose, String phone) { return "sms:verify-lock:" + purpose + ":" + phone; }
    private boolean isProduction() { return List.of(environment.getActiveProfiles()).contains("prod"); }
    private static String randomCode() { return String.format("%06d", RANDOM.nextInt(1_000_000)); }
    private static String currentHour() { return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH")); }
    private static String safeIp(String ip) { return ip == null || ip.isBlank() ? "unknown" : ip.replaceAll("[^0-9A-Fa-f:.]", "_"); }
}
