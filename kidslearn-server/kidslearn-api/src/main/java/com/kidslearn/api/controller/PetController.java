package com.kidslearn.api.controller;

import com.kidslearn.api.service.PetService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "宠物接口")
@RestController
@RequestMapping("/api/v1/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @Operation(summary = "获取我的宠物")
    @GetMapping("/status")
    public R<Map<String, Object>> getMyPet(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.getMyPet(userId));
    }

    @Operation(summary = "喂食宠物")
    @PostMapping("/feed")
    public R<Map<String, Object>> feedPet(HttpServletRequest request, @RequestParam Long petItemId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.feedPet(userId, petItemId));
    }

    @Operation(summary = "和宠物玩耍")
    @PostMapping("/play")
    public R<Map<String, Object>> playWithPet(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.playWithPet(userId));
    }

    @Operation(summary = "宠物换装")
    @PostMapping("/dress")
    public R<Map<String, Object>> dressPet(HttpServletRequest request, @RequestBody List<Long> decorationIds) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.dressPet(userId, decorationIds));
    }

    @Operation(summary = "获取商店道具")
    @GetMapping("/shop")
    public R<List<Map<String, Object>>> getShopItems(@RequestParam(required = false) Integer itemType) {
        return R.ok(petService.getShopItems(itemType));
    }

    @Operation(summary = "购买道具")
    @PostMapping("/shop/buy")
    public R<Map<String, Object>> buyItem(HttpServletRequest request,
                                           @RequestParam Long itemId,
                                           @RequestParam(defaultValue = "1") Integer quantity) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.buyItem(userId, itemId, quantity));
    }

    @Operation(summary = "获取背包")
    @GetMapping("/inventory")
    public R<List<Map<String, Object>>> getInventory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.getInventory(userId));
    }
}
