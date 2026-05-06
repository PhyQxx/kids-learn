export async function claimAchievementReward(receiveReward, achievement, uniApi = globalThis.uni) {
  try {
    const result = await receiveReward(achievement.id)
    achievement.claimed = true
    uniApi.showToast({ title: '奖励已领取！', icon: 'success' })
    return result
  } catch (error) {
    uniApi.showToast({ title: error?.message || '领取失败，请稍后重试', icon: 'none' })
    throw error
  }
}

export async function claimAllAchievementRewards(receiveReward, achievements, uniApi = globalThis.uni) {
  const claimable = achievements.filter(achievement => achievement.status === 'done' && !achievement.claimed)
  if (claimable.length === 0) {
    uniApi.showToast({ title: '暂无可领取奖励', icon: 'none' })
    return { total: 0, success: 0, failed: 0 }
  }

  let success = 0
  let failed = 0
  for (const achievement of claimable) {
    try {
      await receiveReward(achievement.id)
      achievement.claimed = true
      success += 1
    } catch (error) {
      failed += 1
    }
  }

  uniApi.showToast({
    title: failed > 0 ? `已领取${success}个，${failed}个失败` : `已领取${success}个奖励`,
    icon: failed > 0 ? 'none' : 'success'
  })
  return { total: claimable.length, success, failed }
}
