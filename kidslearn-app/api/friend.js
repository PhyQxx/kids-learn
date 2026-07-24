import { get, post } from './request'

// 好友列表
export const getFriendList = () => get('/friend/list')

// 收到的好友请求列表
export const getFriendRequests = () => get('/friend/requests')

// 发送好友请求（后端 add / handle / remove 接口为 @RequestParam 风格，走 query 参数）
export const sendFriendRequest = (friendId) => post(`/friend/add?friendId=${friendId}`)

// 同意 / 拒绝好友请求
export const handleFriendRequest = (requestId, accept) =>
  post(`/friend/handle?requestId=${requestId}&accept=${accept}`)

// 删除好友
export const removeFriend = (friendId) => post(`/friend/remove?friendId=${friendId}`)

// 搜索用户（邀请码 / 用户名 / 昵称）
export const searchUsers = (keyword) => get('/friend/search', { keyword })

// 获取我的邀请码
export const getMyInviteCode = () => get('/friend/my-invite-code')
