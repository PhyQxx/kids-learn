package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.Order;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.OrderMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.impl.SubscriptionPlanCatalog;
import com.kidslearn.api.service.impl.SubscriptionPlan;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "管理后台-订单管理")
@RestController
@RequestMapping("/api/v1/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Operation(summary = "订单列表")
    @GetMapping("/list")
    public R<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        // keyword 仅匹配订单号；用户名为关联表字段，需要时再扩展为 join 查询
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
            .like(keyword != null && !keyword.isEmpty(), Order::getOrderNo, keyword)
            .eq(status != null, Order::getPayStatus, status)
            .orderByDesc(Order::getCreateTime);
        Page<Order> p = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<Order> orders = p.getRecords();
        List<Map<String, Object>> list = Collections.emptyList();
        if (!orders.isEmpty()) {
            // 批量补用户名
            Set<Long> userIds = orders.stream().map(Order::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

            list = new ArrayList<>(orders.size());
            for (Order o : orders) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", o.getId());
                row.put("orderNo", o.getOrderNo());
                row.put("userId", o.getUserId());
                User u = o.getUserId() == null ? null : userMap.get(o.getUserId());
                row.put("username", u == null ? "" : (u.getNickname() != null ? u.getNickname() : u.getUsername()));
                row.put("productType", o.getProductType());
                row.put("productId", o.getProductId());
                row.put("planName", planNameOf(o));
                row.put("amount", o.getAmount() == null ? BigDecimal.ZERO : o.getAmount());
                row.put("payChannel", o.getPayChannel());
                row.put("status", o.getPayStatus());
                row.put("payTime", o.getPayTime());
                row.put("createTime", o.getCreateTime());
                row.put("remark", "");
                list.add(row);
            }
        }
        return R.ok(new PageResult<>(list, p.getTotal(), page, pageSize));
    }

    /** 订阅类订单按 productId(planType) 映射套餐名,其它返回 "-"。 */
    private String planNameOf(Order o) {
        if (o.getProductType() == null || o.getProductType() != OrderServiceImplRef.PRODUCT_TYPE_SUBSCRIPTION
            || o.getProductId() == null) {
            return "-";
        }
        return SubscriptionPlanCatalog.listPlans().stream()
            .filter(pl -> pl.planType().equals(o.getProductId().intValue()))
            .map(SubscriptionPlan::planName)
            .findFirst()
            .orElse("-");
    }

    /** 仅用于引用 OrderServiceImpl 的商品类型常量,避免硬编码魔法数字。 */
    private static final class OrderServiceImplRef {
        static final int PRODUCT_TYPE_SUBSCRIPTION = 1;
    }
}
