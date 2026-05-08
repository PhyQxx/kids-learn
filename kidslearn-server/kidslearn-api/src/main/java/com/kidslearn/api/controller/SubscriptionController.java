package com.kidslearn.api.controller;

import com.kidslearn.api.service.SubscriptionService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "会员订阅接口")
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "获取会员套餐")
    @GetMapping("/plans")
    public R<List<Map<String, Object>>> getPlans() {
        return R.ok(subscriptionService.getPlans());
    }

    @Operation(summary = "获取当前会员状态")
    @GetMapping("/current")
    public R<Map<String, Object>> getCurrentSubscription(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(subscriptionService.getCurrentSubscription(userId));
    }
}

