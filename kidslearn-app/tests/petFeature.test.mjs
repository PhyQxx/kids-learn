import test from 'node:test'
import assert from 'node:assert/strict'

import {
  applyPetPurchaseBalance,
  normalizeDecorations,
  normalizeInventoryItems,
  normalizeShopItems,
  toggleDecorationEquip
} from '../utils/petFeature.mjs'

test('normalizes backpack items into food and mood items with counts', () => {
  const items = normalizeInventoryItems([
    { itemId: 1, itemName: 'Apple', itemType: 1, imageUrl: '', quantity: 2, effectDesc: '+20 hunger' },
    { itemId: 2, itemName: 'Ball', itemType: 2, imageUrl: 'ball', quantity: 0 },
    { itemId: 3, itemName: 'Other', itemType: 9, quantity: 1 }
  ])

  assert.deepEqual(items, [
    { id: 1, name: 'Apple', type: 1, icon: '🍽️', count: 2, effectDesc: '+20 hunger', effectValue: 0, price: 0 },
    { id: 2, name: 'Ball', type: 2, icon: 'ball', count: 0, effectDesc: '', effectValue: 0, price: 0 }
  ])
})

test('normalizes shop items and marks affordability from user balance', () => {
  const items = normalizeShopItems([
    { id: 7, itemName: 'Cake', imageUrl: '', price: 20, effectDesc: 'Sweet', itemType: 1 },
    { id: 8, decoName: 'Hat', imageUrl: 'hat', priceGold: 50, priceDiamond: 0, rarity: 2 }
  ], { gold: 30, diamond: 0 })

  assert.equal(items[0].name, 'Cake')
  assert.equal(items[0].icon, '📦')
  assert.equal(items[0].affordable, true)
  assert.equal(items[1].priceType, 'gold')
  assert.equal(items[1].affordable, false)
})

test('normalizes decorations with owned and equipped state', () => {
  const items = normalizeDecorations(
    [
      { id: 1, decoName: 'Blue Hat', slot: 'head', imageUrl: 'hat' },
      { id: 2, decoName: 'Cape', slot: 'outfit', imageUrl: '' }
    ],
    [{ decorationId: 1 }],
    [1]
  )

  assert.deepEqual(items.map(item => ({
    id: item.id,
    slot: item.slot,
    owned: item.owned,
    equipped: item.equipped,
    icon: item.icon
  })), [
    { id: 1, slot: 'head', owned: true, equipped: true, icon: 'hat' },
    { id: 2, slot: 'outfit', owned: false, equipped: false, icon: '🎭' }
  ])
})

test('toggles one equipped decoration per slot and removes tapped equipped item', () => {
  const source = [
    { id: 1, slot: 'head', owned: true, equipped: true },
    { id: 2, slot: 'head', owned: true, equipped: false },
    { id: 3, slot: 'outfit', owned: true, equipped: true }
  ]

  const equippedNext = toggleDecorationEquip(source, source[1])
  assert.deepEqual(equippedNext.map(item => [item.id, item.equipped]), [
    [1, false],
    [2, true],
    [3, true]
  ])

  const removed = toggleDecorationEquip(equippedNext, equippedNext[1])
  assert.deepEqual(removed.map(item => [item.id, item.equipped]), [
    [1, false],
    [2, false],
    [3, true]
  ])
})

test('applies purchase response balances without guessing when response includes values', () => {
  const current = { gold: 100, diamond: 5, nickname: 'Kid' }

  assert.deepEqual(applyPetPurchaseBalance(current, { gold: 70 }, { priceType: 'gold', price: 20 }), {
    gold: 70,
    diamond: 5,
    nickname: 'Kid'
  })

  assert.deepEqual(applyPetPurchaseBalance(current, {}, { priceType: 'diamond', price: 2 }), {
    gold: 100,
    diamond: 3,
    nickname: 'Kid'
  })
})
