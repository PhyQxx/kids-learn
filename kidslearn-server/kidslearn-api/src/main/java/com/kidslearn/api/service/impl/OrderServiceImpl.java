package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.Order;
import com.kidslearn.api.mapper.OrderMapper;
import com.kidslearn.api.service.OrderService;
import com.kidslearn.api.service.SubscriptionService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    public static final int PRODUCT_TYPE_SUBSCRIPTION = 1;
    public static final int PAY_STATUS_PENDING = 0;
    public static final int PAY_STATUS_PAID = 1;
    public static final int PAY_STATUS_REFUNDED = 2;

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrderMapper orderMapper;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long userId, Integer planType, String payChannel) {
        SubscriptionPlan plan = SubscriptionPlanCatalog.requirePlan(planType);
        Order order = new Order();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setProductType(PRODUCT_TYPE_SUBSCRIPTION);
        order.setProductId(Long.valueOf(plan.planType()));
        order.setAmount(plan.amount());
        order.setPayChannel(payChannel == null || payChannel.isBlank() ? "mock" : payChannel);
        order.setPayStatus(PAY_STATUS_PENDING);
        orderMapper.insert(order);

        Map<String, Object> result = toOrderMap(order);
        result.put("plan", plan.planCode());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> handlePaymentCallback(String orderNo, Integer payStatus) {
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).last("LIMIT 1")
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (order.getPayStatus() != null && order.getPayStatus() == PAY_STATUS_PAID) {
            Map<String, Object> result = toOrderMap(order);
            result.put("subscription", subscriptionService.getCurrentSubscription(order.getUserId()));
            return result;
        }

        if (payStatus == null || (payStatus != PAY_STATUS_PAID && payStatus != PAY_STATUS_REFUNDED)) {
            throw new BusinessException("支付状态无效");
        }

        if (payStatus == PAY_STATUS_REFUNDED) {
            order.setPayStatus(PAY_STATUS_REFUNDED);
            orderMapper.updateById(order);
            return toOrderMap(order);
        }

        order.setPayStatus(PAY_STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        Map<String, Object> result = toOrderMap(order);
        if (order.getProductType() != null && order.getProductType() == PRODUCT_TYPE_SUBSCRIPTION) {
            result.put("subscription", subscriptionService.activateSubscription(
                order.getUserId(),
                order.getProductId().intValue()
            ));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyOrders(Long userId) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
        ).stream().map(this::toOrderMap).toList();
    }

    private String generateOrderNo(Long userId) {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "KL" + LocalDateTime.now().format(ORDER_TIME_FORMAT) + userId + suffix;
    }

    private Map<String, Object> toOrderMap(Order order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("userId", order.getUserId());
        map.put("productType", order.getProductType());
        map.put("productId", order.getProductId());
        map.put("amount", order.getAmount());
        map.put("payChannel", order.getPayChannel());
        map.put("payStatus", order.getPayStatus());
        map.put("payTime", order.getPayTime());
        return map;
    }
}
