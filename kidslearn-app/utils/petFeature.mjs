const DEFAULT_FOOD_ICON = '🍽️'
const DEFAULT_GOODS_ICON = '📦'
const DEFAULT_DECORATION_ICON = '🎭'

const IMAGE_URL_RE = /^(?:https?:|data:|\/\/|\/static\/)/i

/**
 * 判断一个字符串是否为真实图片 URL（而非 emoji 文本）。
 * 形象字段历史上同时承载图片 URL 和 emoji 文本，渲染前需先判定。
 */
export function isImageUrl(value) {
  return typeof value === 'string' && IMAGE_URL_RE.test(value)
}

/**
 * 把形象字段解析为可渲染的结构。
 * 是 URL 返回 { type:'image', src }，是 emoji 返回 { type:'emoji', text }，空值返回 { type:'none' }。
 */
export function resolvePetImage(value, emojiFallback = '') {
  if (isImageUrl(value)) return { type: 'image', src: value }
  const text = typeof value === 'string' && value.trim() ? value : emojiFallback
  return text ? { type: 'emoji', text } : { type: 'none' }
}

function toNumber(value, fallback = 0) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

export function normalizeInventoryItems(items = []) {
  return items
    .filter(item => item.itemType === 1 || item.itemType === 2)
    .map(item => ({
      id: item.itemId,
      name: item.itemName || '',
      type: item.itemType,
      icon: item.imageUrl || DEFAULT_FOOD_ICON,
      count: toNumber(item.quantity),
      effectDesc: item.effectDesc || '',
      effectValue: toNumber(item.effectValue),
      price: toNumber(item.price)
    }))
}

export function normalizeShopItems(items = [], balance = {}) {
  return items.map(item => {
    const isDecoration = item.decoName != null
    const priceGold = toNumber(item.priceGold)
    const priceDiamond = toNumber(item.priceDiamond)
    const priceType = isDecoration && priceGold <= 0 && priceDiamond > 0 ? 'diamond' : 'gold'
    const price = isDecoration
      ? (priceType === 'diamond' ? priceDiamond : priceGold)
      : toNumber(item.price)
    const availableBalance = priceType === 'diamond'
      ? toNumber(balance.diamond)
      : toNumber(balance.gold)

    return {
      id: item.id,
      name: item.itemName || item.decoName || '',
      type: item.itemType || 'decoration',
      slot: item.slot || '',
      icon: item.imageUrl || DEFAULT_GOODS_ICON,
      price,
      priceType,
      effectDesc: item.effectDesc || rarityText(item.rarity),
      rarity: toNumber(item.rarity, 1),
      affordable: availableBalance >= price
    }
  })
}

export function normalizeDecorations(decorations = [], ownedDecorations = [], equippedIds = []) {
  const ownedIds = new Set(ownedDecorations.map(item => item.decorationId))
  const equippedSet = new Set(equippedIds)

  return decorations.map(item => ({
    id: item.id,
    name: item.decoName || '',
    slot: item.slot || '',
    icon: item.imageUrl || DEFAULT_DECORATION_ICON,
    rarity: toNumber(item.rarity, 1),
    owned: ownedIds.has(item.id),
    equipped: equippedSet.has(item.id)
  }))
}

export function toggleDecorationEquip(items = [], target) {
  if (!target?.owned) {
    return items
  }

  return items.map(item => {
    if (item.id === target.id) {
      return { ...item, equipped: !target.equipped }
    }
    if (item.slot === target.slot) {
      return { ...item, equipped: false }
    }
    return item
  })
}

export function applyPetPurchaseBalance(userInfo = {}, response = {}, item = {}) {
  const next = { ...userInfo }
  if (response.gold != null) {
    next.gold = response.gold
  } else if (item.priceType !== 'diamond') {
    next.gold = Math.max(0, toNumber(next.gold) - toNumber(item.price))
  }

  if (response.diamond != null) {
    next.diamond = response.diamond
  } else if (item.priceType === 'diamond') {
    next.diamond = Math.max(0, toNumber(next.diamond) - toNumber(item.price))
  }
  return next
}

function rarityText(rarity) {
  if (rarity === 3) return 'legendary'
  if (rarity === 2) return 'rare'
  return ''
}
