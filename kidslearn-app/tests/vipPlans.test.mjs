import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeVipPlans, vipStatusText } from '../utils/vipPlans.mjs'

test('normalizes backend VIP plans for display', () => {
  const plans = normalizeVipPlans([
    {
      planType: 2,
      planCode: 'annual',
      planName: '年卡',
      amount: 168,
      originalAmount: 300,
      durationDays: 365,
      permanent: false,
      recommended: true,
    },
    {
      planType: 3,
      planCode: 'permanent',
      planName: '永久',
      amount: '298.00',
      originalAmount: '398.00',
      durationDays: 0,
      permanent: true,
      recommended: false,
    },
  ])

  assert.deepEqual(plans.map(plan => ({
    planType: plan.planType,
    code: plan.code,
    name: plan.name,
    priceText: plan.priceText,
    unitText: plan.unitText,
    originalText: plan.originalText,
    featured: plan.featured,
  })), [
    {
      planType: 2,
      code: 'annual',
      name: '年卡',
      priceText: '¥168',
      unitText: '/年',
      originalText: '原价 ¥300',
      featured: true,
    },
    {
      planType: 3,
      code: 'permanent',
      name: '永久',
      priceText: '¥298',
      unitText: '一次',
      originalText: '原价 ¥398',
      featured: false,
    },
  ])
})

test('formats current VIP status text', () => {
  assert.equal(vipStatusText({ active: false }), '当前未开通会员')
  assert.equal(vipStatusText({ active: true, endTime: '2026-12-31T23:59:59' }), '会员有效期至 2026-12-31')
  assert.equal(vipStatusText({ active: true, planType: 3 }), '永久会员已生效')
})
