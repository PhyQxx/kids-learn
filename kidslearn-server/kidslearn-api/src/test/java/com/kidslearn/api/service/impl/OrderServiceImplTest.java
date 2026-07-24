package com.kidslearn.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.kidslearn.api.entity.Order;
import com.kidslearn.api.mapper.OrderMapper;
import com.kidslearn.api.service.SubscriptionService;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderServiceImplTest {

    /**
     * 本测试是纯 Mockito 单测（无 Spring 上下文），而 handlePaymentCallback 用
     * LambdaUpdateWrapper 做乐观锁更新，构造时依赖 MybatisPlus 的 lambda 元数据缓存。
     * 这里手动初始化 Order 实体的表信息缓存，避免 "can not find lambda cache" 报错。
     */
    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            Order.class
        );
    }

    @Test
    void createsPendingSubscriptionOrderFromPlan() {
        OrderMapper orderMapper = org.mockito.Mockito.mock(OrderMapper.class);
        SubscriptionService subscriptionService = org.mockito.Mockito.mock(SubscriptionService.class);
        OrderServiceImpl service = new OrderServiceImpl(orderMapper, subscriptionService);

        Map<String, Object> result = service.createOrder(7L, 2, "mock");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order order = captor.getValue();

        assertEquals(7L, order.getUserId());
        assertEquals(1, order.getProductType());
        assertEquals(2L, order.getProductId());
        assertEquals(new BigDecimal("168.00"), order.getAmount());
        assertEquals("mock", order.getPayChannel());
        assertEquals(0, order.getPayStatus());
        assertEquals(order.getOrderNo(), result.get("orderNo"));
        assertEquals(order.getAmount(), result.get("amount"));
    }

    @Test
    void successfulPaymentCallbackMarksOrderPaidAndActivatesSubscription() {
        OrderMapper orderMapper = org.mockito.Mockito.mock(OrderMapper.class);
        SubscriptionService subscriptionService = org.mockito.Mockito.mock(SubscriptionService.class);
        OrderServiceImpl service = new OrderServiceImpl(orderMapper, subscriptionService);
        Order order = new Order();
        order.setOrderNo("ORD123");
        order.setUserId(7L);
        order.setProductType(1);
        order.setProductId(1L);
        order.setPayStatus(0);
        when(orderMapper.selectOne(any())).thenReturn(order);
        // 支付成功走乐观锁 update(null, LambdaUpdateWrapper)，而非 updateById
        when(orderMapper.update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(subscriptionService.activateSubscription(7L, 1)).thenReturn(Map.of("status", 1));

        Map<String, Object> result = service.handlePaymentCallback("ORD123", 1);

        assertEquals(1, order.getPayStatus());
        verify(orderMapper).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(subscriptionService).activateSubscription(7L, 1);
        assertEquals(1, result.get("payStatus"));
    }

    @Test
    void refundCallbackMarksOrderRefundedWithoutActivatingSubscription() {
        OrderMapper orderMapper = org.mockito.Mockito.mock(OrderMapper.class);
        SubscriptionService subscriptionService = org.mockito.Mockito.mock(SubscriptionService.class);
        OrderServiceImpl service = new OrderServiceImpl(orderMapper, subscriptionService);
        Order order = new Order();
        order.setOrderNo("ORD123");
        order.setUserId(7L);
        order.setProductType(1);
        order.setProductId(1L);
        order.setPayStatus(0);
        when(orderMapper.selectOne(any())).thenReturn(order);

        Map<String, Object> result = service.handlePaymentCallback("ORD123", 2);

        assertEquals(2, order.getPayStatus());
        verify(orderMapper).updateById(order);
        verifyNoInteractions(subscriptionService);
        assertEquals(2, result.get("payStatus"));
    }
}
