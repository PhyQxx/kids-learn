export function getOnboardingPayload(response) {
  if (response && typeof response === 'object' && 'code' in response && 'data' in response) {
    return response.code === 200 ? response.data : null
  }
  return response
}

export function normalizePetOptions(response) {
  const payload = getOnboardingPayload(response)
  return Array.isArray(payload) ? payload : []
}

export function normalizeQuestionOptions(response) {
  const payload = getOnboardingPayload(response)
  return Array.isArray(payload) ? payload : []
}

export function wasAnswerCorrect(response) {
  return !!getOnboardingPayload(response)?.correct
}

