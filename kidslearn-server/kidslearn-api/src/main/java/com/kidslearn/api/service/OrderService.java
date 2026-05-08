package com.kidslearn.api.service;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Map<String, Object> createOrder(Long userId, Integer planType, String payChannel);

    Map<String, Object> handlePaymentCallback(String orderNo, Integer payStatus);

    List<Map<String, Object>> getMyOrders(Long userId);
}

