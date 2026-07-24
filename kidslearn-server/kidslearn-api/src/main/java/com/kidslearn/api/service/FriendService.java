package com.kidslearn.api.service;

import com.kidslearn.api.dto.friend.FriendRequestVO;
import com.kidslearn.api.dto.friend.FriendVO;
import com.kidslearn.api.dto.friend.UserSearchVO;

import java.util.List;

public interface FriendService {

    /** 获取好友列表 */
    List<FriendVO> getFriendList(Long userId);

    /** 获取好友请求列表 */
    List<FriendRequestVO> getFriendRequests(Long userId);

    /** 发送好友请求 */
    void addFriend(Long userId, Long friendId);

    /** 处理好友请求 */
    void handleFriendRequest(Long userId, Long requestId, boolean accept);

    /** 删除好友 */
    void removeFriend(Long userId, Long friendId);

    /** 搜索用户（按邀请码精确查或用户名/昵称模糊查，已脱敏） */
    List<UserSearchVO> searchUsers(Long userId, String keyword);

    /** 获取当前用户的邀请码（若为空则懒生成并落库） */
    String getOrGenerateInviteCode(Long userId);
}
