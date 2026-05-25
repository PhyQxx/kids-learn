import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeParentAiSummary } from '../utils/parentAiSummary.mjs'

test('normalizes parent AI summary with safe list fields', () => {
  const summary = normalizeParentAiSummary({
    summary: '今天学习节奏稳定',
    highlights: ['数学正确率不错', null],
    concerns: '接近时长上限',
    suggestions: ['先休息10分钟', '']
  })

  assert.deepEqual(summary, {
    summary: '今天学习节奏稳定',
    highlights: ['数学正确率不错'],
    concerns: [],
    suggestions: ['先休息10分钟']
  })
})

test('supports wrapped API response and empty fallback', () => {
  const summary = normalizeParentAiSummary({
    data: {
      summary: '本周练习比较均衡',
      suggestions: ['保持固定学习时间']
    }
  })

  assert.equal(summary.summary, '本周练习比较均衡')
  assert.deepEqual(summary.highlights, [])
  assert.deepEqual(summary.concerns, [])
  assert.deepEqual(summary.suggestions, ['保持固定学习时间'])
  assert.deepEqual(normalizeParentAiSummary(null), {
    summary: '',
    highlights: [],
    concerns: [],
    suggestions: []
  })
})
