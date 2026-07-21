package com.kidslearn.api.service;

import com.kidslearn.api.dto.friend.FriendRequestVO;
import com.kidslearn.api.dto.friend.FriendVO;

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
}
