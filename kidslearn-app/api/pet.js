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

// 洗澡
export const bathPet = () => post('/pet/bath')

// 换装 (body: List<Long> 装饰ID数组)
export const dressPet = (decorationIds) => post('/pet/dress', decorationIds)

// 购买道具 (itemId, quantity 通过 query param)
export const buyItem = (itemId, quantity = 1) => post(`/pet/shop/buy?itemId=${itemId}&quantity=${quantity}`)

// 获取装饰品列表
export const getDecorations = (slot) => get('/pet/decorations', { slot })

// 购买装饰品
export const buyDecoration = (decorationId) => post(`/pet/decorations/buy?decorationId=${decorationId}`)

// 获取已拥有装饰品
export const getDecorationInventory = () => get('/pet/decorations/inventory')

// 初始化宠物
export const initPet = () => post('/pet/init')

// 获取可选宠物列表（新手引导）
export const getAvailablePets = () => get('/pet/available')

// 选择宠物（新手引导）
export const selectPet = (petId) => post(`/pet/select?petId=${petId}`)

// 更新新手引导步骤
export const updateOnboardingStep = (step) => post(`/user/onboarding-step?step=${step}`)
