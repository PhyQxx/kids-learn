package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.dto.friend.FriendRequestVO;
import com.kidslearn.api.dto.friend.FriendVO;
import com.kidslearn.api.entity.Friend;
import com.kidslearn.api.entity.User;
import com.kidslearn.api.mapper.FriendMapper;
import com.kidslearn.api.mapper.UserMapper;
import com.kidslearn.api.service.FriendService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

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
        if (targetUser == null || Integer.valueOf(0).equals(targetUser.getStatus())) {
            throw new BusinessException("用户不存在");
        }

        Long count = friendMapper.selectCount(
            new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, 1)
        );
        if (count > 0) {
            throw new BusinessException("已经是好友了");
        }

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
    }

    @Override
    @Transactional
    public void handleFriendRequest(Long userId, Long requestId, boolean accept) {
        Friend friendRequest = friendMapper.selectById(requestId);
        if (friendRequest == null || !friendRequest.getFriendId().equals(userId)) {
            throw new BusinessException("请求不存在");
        }

        if (accept) {
            friendRequest.setStatus(1);
            friendRequest.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(friendRequest);

            Friend reverse = new Friend();
            reverse.setUserId(userId);
            reverse.setFriendId(friendRequest.getUserId());
            reverse.setStatus(1);
            reverse.setCreateTime(LocalDateTime.now());
            friendMapper.insert(reverse);
        } else {
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
}
