package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kidslearn.api.entity.Order;
import com.kidslearn.api.mapper.OrderMapper;
import com.kidslearn.api.service.OrderService;
import com.kidslearn.api.service.SubscriptionService;
import com.kidslearn.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    public static final int PRODUCT_TYPE_SUBSCRIPTION = 1;
    public static final int PAY_STATUS_PENDING = 0;
    public static final int PAY_STATUS_PAID = 1;
    public static final int PAY_STATUS_REFUNDED = 2;
    /** 订单已超时关闭（由定时任务清理未支付订单时写入） */
    public static final int PAY_STATUS_CLOSED = 3;

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrderMapper orderMapper;
    private final SubscriptionService subscriptionService;
    private final Environment environment;
    private final boolean paymentEnabled;
    private final java.util.Set<String> allowedChannels;

    @Autowired
    public OrderServiceImpl(OrderMapper orderMapper, SubscriptionService subscriptionService,
            Environment environment,
            @Value("${kidslearn.payment.enabled:false}") boolean paymentEnabled,
            @Value("${kidslearn.payment.allowed-channels:}") String allowedChannels) {
        this.orderMapper = orderMapper;
        this.subscriptionService = subscriptionService;
        this.environment = environment;
        this.paymentEnabled = paymentEnabled;
        this.allowedChannels = java.util.Arrays.stream(allowedChannels.split(","))
            .map(String::trim).filter(value -> !value.isBlank()).map(String::toLowerCase)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    OrderServiceImpl(OrderMapper orderMapper, SubscriptionService subscriptionService) {
        this.orderMapper = orderMapper;
        this.subscriptionService = subscriptionService;
        this.environment = null;
        this.paymentEnabled = true;
        this.allowedChannels = java.util.Set.of("mock", "wechat", "alipay", "apple");
    }

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long userId, Integer planType, String payChannel) {
        String normalizedChannel = payChannel == null ? "" : payChannel.trim().toLowerCase();
        if (!paymentEnabled || normalizedChannel.isBlank() || !allowedChannels.contains(normalizedChannel)) {
            throw new BusinessException("会员购买功能筹备中");
        }
        if (isProduction() && "mock".equals(normalizedChannel)) {
            throw new BusinessException("生产环境禁止模拟支付");
        }
        SubscriptionPlan plan = SubscriptionPlanCatalog.requirePlan(planType);
        Order order = new Order();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setProductType(PRODUCT_TYPE_SUBSCRIPTION);
        order.setProductId(Long.valueOf(plan.planType()));
        order.setAmount(plan.amount());
        order.setPayChannel(normalizedChannel);
        order.setPayStatus(PAY_STATUS_PENDING);
        orderMapper.insert(order);

        Map<String, Object> result = toOrderMap(order);
        result.put("plan", plan.planCode());
        return result;
    }

    private boolean isProduction() {
        return environment != null && java.util.List.of(environment.getActiveProfiles()).contains("prod");
    }

    @Override
    @Transactional
    public Map<String, Object> handlePaymentCallback(String orderNo, Integer payStatus) {
        // 幂等性检查：使用订单号+状态作为唯一键，防止重复处理
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).last("LIMIT 1")
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 幂等性检查：如果订单已处理（已支付或已退款），直接返回当前状态，不重复处理
        if (order.getPayStatus() != null) {
            if (order.getPayStatus() == PAY_STATUS_PAID) {
                log.info("订单已支付，跳过重复回调: orderNo={}", orderNo);
                Map<String, Object> result = toOrderMap(order);
                result.put("subscription", subscriptionService.getCurrentSubscription(order.getUserId()));
                return result;
            }
            if (order.getPayStatus() == PAY_STATUS_REFUNDED) {
                log.info("订单已退款，跳过重复回调: orderNo={}", orderNo);
                return toOrderMap(order);
            }
            // 订单已被定时任务超时关闭，忽略迟到的支付回调，防止错误激活订阅
            if (order.getPayStatus() == PAY_STATUS_CLOSED) {
                log.warn("订单已超时关闭，忽略迟到回调: orderNo={}", orderNo);
                throw new BusinessException("订单已超时关闭，请重新下单");
            }
        }

        if (payStatus == null || (payStatus != PAY_STATUS_PAID && payStatus != PAY_STATUS_REFUNDED)) {
            throw new BusinessException("支付状态无效");
        }

        if (payStatus == PAY_STATUS_REFUNDED) {
            order.setPayStatus(PAY_STATUS_REFUNDED);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("订单退款成功: orderNo={}", orderNo);
            return toOrderMap(order);
        }

        // 支付成功：用乐观锁更新订单状态（WHERE pay_status=0），并发回调只有一个能成功激活
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
            .eq(Order::getId, order.getId())
            .eq(Order::getPayStatus, PAY_STATUS_PENDING)
            .set(Order::getPayStatus, PAY_STATUS_PAID)
            .set(Order::getPayTime, now)
            .set(Order::getUpdateTime, now));
        if (rows == 0) {
            // 并发竞争失败：可能已被另一个回调处理（PAID）或被定时任务关闭（CLOSED），重读返回最新状态
            log.warn("支付回调并发竞争失败，订单已被处理: orderNo={}", orderNo);
            Order latest = orderMapper.selectById(order.getId());
            if (latest != null && latest.getPayStatus() != null && latest.getPayStatus() == PAY_STATUS_PAID) {
                Map<String, Object> result = toOrderMap(latest);
                result.put("subscription", subscriptionService.getCurrentSubscription(latest.getUserId()));
                return result;
            }
            throw new BusinessException("订单状态更新失败，可能已被关闭");
        }

        order.setPayStatus(PAY_STATUS_PAID);
        order.setPayTime(now);

        // 激活订阅（如果是订阅订单）
        Map<String, Object> result = toOrderMap(order);
        if (order.getProductType() != null && order.getProductType() == PRODUCT_TYPE_SUBSCRIPTION) {
            result.put("subscription", subscriptionService.activateSubscription(
                order.getUserId(),
                order.getProductId().intValue()
            ));
        }

        log.info("订单支付处理成功: orderNo={}, userId={}", orderNo, order.getUserId());
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
