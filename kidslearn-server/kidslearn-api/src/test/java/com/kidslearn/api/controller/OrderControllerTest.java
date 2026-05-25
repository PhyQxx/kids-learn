package com.kidslearn.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidslearn.api.dto.order.PaymentCallbackDTO;
import com.kidslearn.api.service.OrderService;
import com.kidslearn.api.service.impl.PaymentCallbackVerifier;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.result.R;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderControllerTest {

    @Test
    void verifiesPaymentCallbackBeforeMutatingOrder() {
        OrderService orderService = mock(OrderService.class);
        PaymentCallbackVerifier verifier = mock(PaymentCallbackVerifier.class);
        OrderController controller = new OrderController(orderService, verifier);
        PaymentCallbackDTO dto = callback();
        when(orderService.handlePaymentCallback("KL123", 1)).thenReturn(Map.of("orderNo", "KL123"));

        R<Map<String, Object>> result = controller.payCallback(dto);

        assertEquals(200, result.getCode());
        verify(verifier).verify(dto);
        verify(orderService).handlePaymentCallback("KL123", 1);
    }

    @Test
    void doesNotMutateOrderWhenPaymentCallbackVerificationFails() {
        OrderService orderService = mock(OrderService.class);
        PaymentCallbackVerifier verifier = mock(PaymentCallbackVerifier.class);
        OrderController controller = new OrderController(orderService, verifier);
        PaymentCallbackDTO dto = callback();
        org.mockito.Mockito.doThrow(new BusinessException("bad signature")).when(verifier).verify(dto);

        assertThrows(BusinessException.class, () -> controller.payCallback(dto));

        verify(orderService, never()).handlePaymentCallback(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private PaymentCallbackDTO callback() {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setOrderNo("KL123");
        dto.setPayStatus(1);
        dto.setTimestamp(1780000000000L);
        dto.setSignature("signature");
        return dto;
    }
}
