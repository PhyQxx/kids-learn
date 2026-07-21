import { get, post } from './request'

// 获取通知列表
export const getNotificationList = (page = 1, pageSize = 20) => get('/notification/list', { page, pageSize })

// 获取未读数量
export const getUnreadCount = () => get('/notification/unread-count')

// 标记单条已读
export const markAsRead = (notificationId) => post('/notification/read?notificationId=' + notificationId)

// 全部已读
export const markAllAsRead = () => post('/notification/read-all')
