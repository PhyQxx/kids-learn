package com.kidslearn.api.controller;

import com.kidslearn.api.dto.friend.FriendRequestVO;
import com.kidslearn.api.dto.friend.FriendVO;
import com.kidslearn.api.service.FriendService;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "好友接口")
@RestController
@RequestMapping("/api/v1/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "获取好友列表")
    @GetMapping("/list")
    public R<List<FriendVO>> getFriendList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(friendService.getFriendList(userId));
    }

    @Operation(summary = "获取好友请求列表")
    @GetMapping("/requests")
    public R<List<FriendRequestVO>> getFriendRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(friendService.getFriendRequests(userId));
    }

    @Operation(summary = "发送好友请求")
    @PostMapping("/add")
    public R<Void> addFriend(HttpServletRequest request, @RequestParam Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.addFriend(userId, friendId);
        return R.ok();
    }

    @Operation(summary = "处理好友请求")
    @PostMapping("/handle")
    public R<Void> handleFriendRequest(HttpServletRequest request, @RequestParam Long requestId, @RequestParam boolean accept) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.handleFriendRequest(userId, requestId, accept);
        return R.ok();
    }

    @Operation(summary = "删除好友")
    @PostMapping("/remove")
    public R<Void> removeFriend(HttpServletRequest request, @RequestParam Long friendId) {
        Long userId = (Long) request.getAttribute("userId");
        friendService.removeFriend(userId, friendId);
        return R.ok();
    }
}
