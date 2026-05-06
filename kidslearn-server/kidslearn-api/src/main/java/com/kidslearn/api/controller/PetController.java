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

    @Operation(summary = "宠物洗澡")
    @PostMapping("/bath")
    public R<Map<String, Object>> bathPet(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.bathPet(userId));
    }

    @Operation(summary = "获取装饰品列表")
    @GetMapping("/decorations")
    public R<List<Map<String, Object>>> getDecorations(
            @RequestParam(required = false) String slot) {
        return R.ok(petService.getDecorations(slot));
    }

    @Operation(summary = "购买装饰品")
    @PostMapping("/decorations/buy")
    public R<Map<String, Object>> buyDecoration(
            HttpServletRequest request,
            @RequestParam Long decorationId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.buyDecoration(userId, decorationId));
    }

    @Operation(summary = "获取已拥有装饰品")
    @GetMapping("/decorations/inventory")
    public R<List<Map<String, Object>>> getDecorationInventory(
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.getDecorationInventory(userId));
    }

    @Operation(summary = "初始化宠物")
    @PostMapping("/init")
    public R<Map<String, Object>> initPet(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.initPet(userId));
    }

    @Operation(summary = "获取可选宠物列表")
    @GetMapping("/available")
    public R<List<Map<String, Object>>> getAvailablePets() {
        return R.ok(petService.getAvailablePets());
    }

    @Operation(summary = "选择宠物（新手引导）")
    @PostMapping("/select")
    public R<Map<String, Object>> selectPet(HttpServletRequest request, @RequestParam Long petId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(petService.selectPet(userId, petId));
    }
}
