import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const files = [
  'components/home/HomeContent.vue',
  'components/ranking/RankingContent.vue',
  'components/achievement/AchievementContent.vue',
  'pages/mine/vip.vue',
]

test('production pages do not ship mock data fallbacks', () => {
  for (const file of files) {
    const source = readFileSync(new URL(`../${file}`, import.meta.url), 'utf8')
    assert.equal(source.includes('function applyMockData'), false, `${file} still declares applyMockData`)
    assert.equal(source.includes('fallbackPlans'), false, `${file} still declares fallbackPlans`)
    assert.equal(source.includes("createOrder(plan.planType, 'mock')"), false, `${file} still forces mock payment`)
  }
})
