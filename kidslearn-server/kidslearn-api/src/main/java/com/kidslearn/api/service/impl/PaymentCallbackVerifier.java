package com.kidslearn.api.service.impl;

import com.kidslearn.api.dto.order.PaymentCallbackDTO;
import com.kidslearn.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentCallbackVerifier {

    private static final long ALLOWED_DRIFT_MILLIS = 5 * 60 * 1000L;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final Clock clock;

    @Autowired
    public PaymentCallbackVerifier(@Value("${payment.callback.secret:}") String secret) {
        this(secret, Clock.systemUTC());
    }

    PaymentCallbackVerifier(String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
    }

    public void verify(PaymentCallbackDTO dto) {
        if (secret == null || secret.isBlank()) {
            throw new BusinessException("Payment callback secret is not configured");
        }
        if (dto == null || dto.getTimestamp() == null || dto.getSignature() == null || dto.getSignature().isBlank()) {
            throw new BusinessException("Invalid payment callback signature");
        }
        long drift = Math.abs(clock.millis() - dto.getTimestamp());
        if (drift > ALLOWED_DRIFT_MILLIS) {
            throw new BusinessException("Payment callback timestamp expired");
        }

        String expected = sign(dto, secret);
        if (!MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            dto.getSignature().getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BusinessException("Invalid payment callback signature");
        }
    }

    static String sign(PaymentCallbackDTO dto, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload(dto).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("Payment callback signature failed");
        }
    }

    private static String payload(PaymentCallbackDTO dto) {
        return dto.getOrderNo() + "|" + dto.getPayStatus() + "|" + dto.getTimestamp();
    }
}
