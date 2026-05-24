export function isUnauthorizedResponse(response) {
  return Number(response?.statusCode) === 401 || Number(response?.data?.code) === 401
}

export function isRefreshTokenRequest(url) {
  return String(url || '').includes('/auth/refresh-token')
}

export function shouldAttemptTokenRefresh(response, url) {
  return isUnauthorizedResponse(response) && !isRefreshTokenRequest(url)
}

export function handleUnauthorizedResponse(response, userStore) {
  if (!isUnauthorizedResponse(response)) {
    return false
  }

  userStore.logout()
  return true
}
