package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kidslearn.api.dto.order.PaymentCallbackDTO;
import com.kidslearn.common.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PaymentCallbackVerifierTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-24T04:00:00Z"), ZoneOffset.UTC);

    @Test
    void acceptsCallbackWithValidSignatureInsideTimeWindow() {
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier("callback-secret", FIXED_CLOCK);
        PaymentCallbackDTO dto = callback(1, Instant.parse("2026-05-24T03:59:30Z").toEpochMilli());
        dto.setSignature(PaymentCallbackVerifier.sign(dto, "callback-secret"));

        assertDoesNotThrow(() -> verifier.verify(dto));
    }

    @Test
    void rejectsCallbackWithInvalidSignature() {
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier("callback-secret", FIXED_CLOCK);
        PaymentCallbackDTO dto = callback(1, Instant.parse("2026-05-24T03:59:30Z").toEpochMilli());
        dto.setSignature("bad-signature");

        assertThrows(BusinessException.class, () -> verifier.verify(dto));
    }

    @Test
    void rejectsCallbackOutsideTimeWindow() {
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier("callback-secret", FIXED_CLOCK);
        PaymentCallbackDTO dto = callback(1, Instant.parse("2026-05-24T03:40:00Z").toEpochMilli());
        dto.setSignature(PaymentCallbackVerifier.sign(dto, "callback-secret"));

        assertThrows(BusinessException.class, () -> verifier.verify(dto));
    }

    private PaymentCallbackDTO callback(Integer payStatus, long timestamp) {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setOrderNo("ORD123");
        dto.setPayStatus(payStatus);
        dto.setTimestamp(timestamp);
        return dto;
    }
}
