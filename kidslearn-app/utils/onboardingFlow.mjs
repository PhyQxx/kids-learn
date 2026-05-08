const ONBOARDING_URL = '/pages/onboarding/index'
const MAIN_URL = '/pages/main/index'

export function getPostAuthRedirectUrl(userInfo) {
  const step = Number(userInfo?.onboardingStep ?? 0)
  return step < 3 ? ONBOARDING_URL : MAIN_URL
}

