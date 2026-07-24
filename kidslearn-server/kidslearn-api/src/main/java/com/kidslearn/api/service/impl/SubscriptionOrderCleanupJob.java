package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kidslearn.api.entity.Order;
import com.kidslearn.api.entity.Subscription;
import com.kidslearn.api.mapper.OrderMapper;
import com.kidslearn.api.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 定时清理任务：
 * 1) 把已过期的订阅 status 从 ACTIVE 翻新为 EXPIRED（权益判定仍由 endTime 兜底，此处仅为数据一致性与运营统计）。
 * 2) 关闭超过阈值仍未支付的订单，避免 PENDING 订单无限堆积。
 *
 * 采用 Redis 简单互斥锁防止多实例重复执行（项目无 redisson/shedlock 依赖）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionOrderCleanupJob {

    private static final String LOCK_KEY = "kidslearn:job:cleanup-lock";
    private static final long LOCK_TTL_SECONDS = 120;

    private final SubscriptionMapper subscriptionMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${order.cleanup.timeout-minutes:30}")
    private int orderTimeoutMinutes;

    /**
     * 每 5 分钟整点执行一次。
     * 注意：Spring 的 @Scheduled 中 initialDelay 不支持与 cron 触发器同时使用，
     * 因此用 5 分钟整点 cron 本身的等待间隔来避让启动高峰（首次执行最早在下一个整 5 分）。
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void cleanup() {
        Boolean locked = stringRedisTemplate.opsForValue()
            .setIfAbsent(LOCK_KEY, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
            expireSubscriptions();
            closeTimeoutOrders();
        } catch (Exception e) {
            log.error("订阅/订单清理任务异常", e);
        } finally {
            stringRedisTemplate.delete(LOCK_KEY);
        }
    }

    /**
     * WHERE status=1 AND end_time < now → status=2。
     * 注意：永久订阅 endTime 为 9999-12-31，不会被命中。
     */
    private void expireSubscriptions() {
        int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<Subscription>()
            .eq(Subscription::getStatus, SubscriptionActivationEngine.STATUS_ACTIVE)
            .lt(Subscription::getEndTime, LocalDateTime.now())
            .set(Subscription::getStatus, SubscriptionActivationEngine.STATUS_EXPIRED));
        if (rows > 0) {
            log.info("订阅过期翻新: {} 条", rows);
        }
    }

    /**
     * WHERE pay_status=0 AND create_time < (now - timeout) → pay_status=3(CLOSED)。
     */
    private void closeTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(orderTimeoutMinutes);
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
            .eq(Order::getPayStatus, OrderServiceImpl.PAY_STATUS_PENDING)
            .lt(Order::getCreateTime, cutoff)
            .set(Order::getPayStatus, OrderServiceImpl.PAY_STATUS_CLOSED));
        if (rows > 0) {
            log.info("超时未支付订单关闭: {} 条", rows);
        }
    }
}
