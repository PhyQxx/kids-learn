import { get, post } from './request'

// 获取宠物状态
export const getPetStatus = () => get('/pet/status')

// 获取商店列表
export const getShopItems = (itemType) => get('/pet/shop', { itemType })

// 获取背包
export const getInventory = () => get('/pet/inventory')

// 喂食 (petItemId 通过 query param)
export const feedPet = (petItemId) => post('/pet/feed?petItemId=' + petItemId)

// 玩耍
export const playPet = () => post('/pet/play')

// 换装 (body: List<Long> 装饰ID数组)
export const dressPet = (decorationIds) => post('/pet/dress', decorationIds)

// 购买道具 (itemId, quantity 通过 query param)
export const buyItem = (itemId, quantity = 1) => post(`/pet/shop/buy?itemId=${itemId}&quantity=${quantity}`)
