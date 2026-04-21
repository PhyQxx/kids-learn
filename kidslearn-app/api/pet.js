import { get, post } from './request'

// 获取宠物状态
export const getPetStatus = () => get('/pet/status')

// 获取商店列表
export const getShopItems = () => get('/pet/shop')

// 获取背包
export const getInventory = () => get('/pet/inventory')

// 喂食
export const feedPet = (data) => post('/pet/feed', data)

// 玩耍
export const playPet = (data) => post('/pet/play', data)

// 换装
export const dressPet = (data) => post('/pet/dress', data)

// 购买道具
export const buyItem = (data) => post('/pet/shop/buy', data)
