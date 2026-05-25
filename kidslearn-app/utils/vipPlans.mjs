const UNIT_BY_CODE = {
  monthly: '/月',
  annual: '/年',
  permanent: '一次',
}

export function normalizeVipPlans(plans = []) {
  if (!Array.isArray(plans)) return []
  return plans.map(plan => {
    const amount = toNumber(plan.amount, 0)
    const originalAmount = toNumber(plan.originalAmount, 0)
    const code = textOr(plan.planCode, '')
    return {
      planType: toNumber(plan.planType, 0),
      code,
      name: textOr(plan.planName, '会员套餐'),
      amount,
      priceText: `¥${formatAmount(amount)}`,
      unitText: plan.permanent ? '一次' : UNIT_BY_CODE[code] || `/${toNumber(plan.durationDays, 0)}天`,
      originalText: originalAmount > 0 ? `原价 ¥${formatAmount(originalAmount)}` : '',
      durationDays: toNumber(plan.durationDays, 0),
      permanent: Boolean(plan.permanent),
      featured: Boolean(plan.recommended),
    }
  })
}

export function vipStatusText(subscription = null) {
  if (!subscription || !subscription.active) return '当前未开通会员'
  if (Number(subscription.planType) === 3) return '永久会员已生效'
  const date = textOr(subscription.endTime, '').slice(0, 10)
  return date ? `会员有效期至 ${date}` : '会员已生效'
}

function formatAmount(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2)
}

function textOr(value, fallback) {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

function toNumber(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}
