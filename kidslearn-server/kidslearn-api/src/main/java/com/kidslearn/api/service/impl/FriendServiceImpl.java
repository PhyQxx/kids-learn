package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kidslearn.api.dto.friend.FriendRequestVO;
import com.kidslearn.api.dto.friend.FriendVO;
import com.kidslearn.api.dto.friend.UserSearchVO;
import com.kidslearn.api.entity.Friend;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.FriendMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.FriendService;
import com.kidslearn.api.service.NotificationEventService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final NotificationEventService notificationEventService;

    @Override
    public List<FriendVO> getFriendList(Long userId) {
        List<Friend> friends = friendMapper.selectList(
            new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 1)
        );

        if (friends.isEmpty()) {
            return List.of();
        }

        List<Long> friendIds = friends.stream().map(Friend::getFriendId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(friendIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        List<FriendVO> result = new ArrayList<>();
        for (Friend f : friends) {
            User u = userMap.get(f.getFriendId());
            if (u != null && !Integer.valueOf(0).equals(u.getStatus())) {
                FriendVO vo = new FriendVO();
                vo.setFriendId(u.getId());
                vo.setNickname(u.getNickname());
                vo.setAvatar(u.getAvatar());
                vo.setLevel(u.getLevel());
                vo.setAddTime(f.getCreateTime());
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public List<FriendRequestVO> getFriendRequests(Long userId) {
        List<Friend> requests = friendMapper.selectList(
            new LambdaQueryWrapper<Friend>()
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0)
        );

        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> senderIds = requests.stream().map(Friend::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        List<FriendRequestVO> result = new ArrayList<>();
        for (Friend r : requests) {
            User u = userMap.get(r.getUserId());
            if (u != null && !Integer.valueOf(0).equals(u.getStatus())) {
                FriendRequestVO vo = new FriendRequestVO();
                vo.setRequestId(r.getId());
                vo.setUserId(u.getId());
                vo.setNickname(u.getNickname());
                vo.setAvatar(u.getAvatar());
                vo.setRequestTime(r.getCreateTime());
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new BusinessException("不能添加自己为好友");
        }

        User targetUser = userMapper.selectById(friendId);
        // 过滤已注销账号和管理员账号（userType=3），避免管理员被加好友刷屏
        if (targetUser == null
            || Integer.valueOf(0).equals(targetUser.getStatus())
            || Integer.valueOf(3).equals(targetUser.getUserType())) {
            throw new BusinessException("用户不存在");
        }

        // 已是好友（任意方向 status=1）则幂等提示
        Long acceptedCount = friendMapper.selectCount(
            new LambdaQueryWrapper<Friend>()
                .and(w -> w
                    .and(inner -> inner.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId))
                    .or(inner -> inner.eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId)))
                .eq(Friend::getStatus, 1));
        if (acceptedCount > 0) {
            throw new BusinessException("已经是好友了");
        }

        // 对方已向我发过请求（反向 pending）：直接互相接受，建立双向好友，避免后续 accept 撞唯一键
        Friend reversePending = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId).eq(Friend::getStatus, 0).last("LIMIT 1"));
        if (reversePending != null) {
            // 接受对方的反向请求
            reversePending.setStatus(1);
            reversePending.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(reversePending);
            // 插入正向行（此时正向行不存在，不会撞唯一键）
            Friend forward = new Friend();
            forward.setUserId(userId);
            forward.setFriendId(friendId);
            forward.setStatus(1);
            forward.setCreateTime(LocalDateTime.now());
            friendMapper.insert(forward);
            return;
        }

        // 已发过正向请求则幂等提示
        Long requestCount = friendMapper.selectCount(
            new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, 0)
        );
        if (requestCount > 0) {
            throw new BusinessException("已发送过好友请求");
        }

        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(0);
        friend.setCreateTime(LocalDateTime.now());
        friendMapper.insert(friend);

        // 给被添加方发送好友请求通知，对方可在通知中心直接跳转处理
        User requester = userMapper.selectById(userId);
        String requesterName = requester != null && requester.getNickname() != null
            ? requester.getNickname() : "有一位小伙伴";
        notificationEventService.publish(
            "friend-request:" + friend.getId() + ":" + friendId,
            friendId, "FRIEND_REQUEST", "好友请求",
            requesterName + " 请求添加你为好友",
            "OPEN_FRIEND_REQUESTS", String.valueOf(friend.getId()),
            LocalDateTime.now().plusDays(30));
    }

    @Override
    @Transactional
    public void handleFriendRequest(Long userId, Long requestId, boolean accept) {
        Friend friendRequest = friendMapper.selectById(requestId);
        if (friendRequest == null || !friendRequest.getFriendId().equals(userId)) {
            throw new BusinessException("请求不存在");
        }

        if (accept) {
            // status 守卫：已处理的请求幂等返回，防重复 accept 撞唯一键
            if (Integer.valueOf(1).equals(friendRequest.getStatus())) {
                return;
            }
            friendRequest.setStatus(1);
            friendRequest.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(friendRequest);

            // 插入反向行：若对方也向我发过 pending 请求则更新它，否则 insert（捕获唯一键冲突幂等处理）
            Friend reversePending = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendRequest.getUserId())
                .last("LIMIT 1"));
            if (reversePending != null) {
                if (Integer.valueOf(0).equals(reversePending.getStatus())) {
                    reversePending.setStatus(1);
                    reversePending.setUpdateTime(LocalDateTime.now());
                    friendMapper.updateById(reversePending);
                }
            } else {
                Friend reverse = new Friend();
                reverse.setUserId(userId);
                reverse.setFriendId(friendRequest.getUserId());
                reverse.setStatus(1);
                reverse.setCreateTime(LocalDateTime.now());
                try {
                    friendMapper.insert(reverse);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // 反向行已存在（并发），幂等忽略
                }
            }

            // 通知请求发起方：对方已接受
            User accepter = userMapper.selectById(userId);
            String accepterName = accepter != null && accepter.getNickname() != null
                ? accepter.getNickname() : "你的好友";
            notificationEventService.publish(
                "friend-result-accept:" + requestId + ":" + friendRequest.getUserId(),
                friendRequest.getUserId(), "FRIEND_RESULT", "好友请求已通过",
                accepterName + " 已接受你的好友请求，快去发起挑战吧",
                "OPEN_FRIEND_LIST", null,
                LocalDateTime.now().plusDays(30));
        } else {
            // reject 守卫：只能拒绝待处理请求（status=0），防止误删已建立的好友关系
            if (!Integer.valueOf(0).equals(friendRequest.getStatus())) {
                throw new BusinessException("该请求已处理");
            }
            friendMapper.deleteById(requestId);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        friendMapper.delete(
            new LambdaQueryWrapper<Friend>()
                .and(w -> w
                    .and(inner -> inner.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId))
                    .or(inner -> inner.eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId))
                )
        );
    }

    @Override
    public List<UserSearchVO> searchUsers(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String kw = keyword.trim();
        if (kw.length() > 20) {
            kw = kw.substring(0, 20);
        }
        final String finalKw = kw;
        // 转义 LIKE 通配符，防止用 % / _ 枚举用户库
        final String escapedKw = kw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        // 邀请码精确匹配 或 用户名/昵称模糊匹配；过滤已注销账号和管理员账号（userType=3）
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
            .and(w -> w.eq(User::getInviteCode, finalKw)
                .or().eq(User::getUsername, finalKw)
                .or().like(User::getNickname, escapedKw))
            .ne(User::getId, userId)
            .ne(User::getStatus, 0)
            .ne(User::getUserType, 3)
            .last("LIMIT 20"));

        if (users.isEmpty()) {
            return List.of();
        }

        // 批量查好友关系，标记 isFriend（已是好友或已发请求均算"已有关系"）
        List<Long> targetIds = users.stream().map(User::getId).collect(Collectors.toList());
        Set<Long> relatedIds = new HashSet<>(friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).in(Friend::getFriendId, targetIds))
            .stream().map(Friend::getFriendId).collect(Collectors.toSet()));

        List<UserSearchVO> result = new ArrayList<>();
        for (User u : users) {
            UserSearchVO vo = new UserSearchVO();
            vo.setUserId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setLevel(u.getLevel());
            vo.setInviteCode(u.getInviteCode());
            vo.setIsFriend(relatedIds.contains(u.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public String getOrGenerateInviteCode(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getInviteCode() != null && !user.getInviteCode().trim().isEmpty()) {
            return user.getInviteCode();
        }
        // 懒生成：最多重试 5 次以避开极小概率的邀请码冲突
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = InviteCodeGenerator.generate();
            // 原子更新：仅当当前 invite_code 为空或空白时写入，利用唯一索引兜底并发
            int updated = userMapper.update(null, new UpdateWrapper<User>()
                .eq("id", userId)
                .and(w -> w.isNull("invite_code").or().eq("invite_code", "").or().eq("invite_code", " "))
                .set("invite_code", code));
            if (updated == 1) {
                return code;
            }
            // updated==0 可能是并发被别人抢先写入，重读一次
            User fresh = userMapper.selectById(userId);
            if (fresh != null && fresh.getInviteCode() != null && !fresh.getInviteCode().trim().isEmpty()) {
                return fresh.getInviteCode();
            }
            // 否则可能是唯一索引冲突，换一个码重试
        }
        log.warn("生成邀请码重试 5 次仍未成功, userId={}", userId);
        throw new BusinessException("邀请码生成失败，请稍后重试");
    }
}
