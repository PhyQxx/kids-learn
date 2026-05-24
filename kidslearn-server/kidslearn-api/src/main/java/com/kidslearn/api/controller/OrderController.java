package com.kidslearn.api.controller;

import com.kidslearn.api.dto.order.CreateOrderDTO;
import com.kidslearn.api.dto.order.PaymentCallbackDTO;
import com.kidslearn.api.service.OrderService;
import com.kidslearn.api.service.impl.PaymentCallbackVerifier;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "订单支付接口")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentCallbackVerifier paymentCallbackVerifier;

    @Operation(summary = "创建会员订单")
    @PostMapping("/create")
    public R<Map<String, Object>> createOrder(HttpServletRequest request, @Valid @RequestBody CreateOrderDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(orderService.createOrder(userId, dto.getPlanType(), dto.getPayChannel()));
    }

    @Operation(summary = "支付回调")
    @PostMapping("/pay-callback")
    public R<Map<String, Object>> payCallback(@Valid @RequestBody PaymentCallbackDTO dto) {
        paymentCallbackVerifier.verify(dto);
        return R.ok(orderService.handlePaymentCallback(dto.getOrderNo(), dto.getPayStatus()));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/my")
    public R<List<Map<String, Object>>> getMyOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(orderService.getMyOrders(userId));
    }
}

