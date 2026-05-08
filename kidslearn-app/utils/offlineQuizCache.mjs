const QUESTION_CACHE_PREFIX = 'kidslearn:quiz-questions:v1'
const AUDIO_CACHE_KEY = 'kidslearn:audio-files:v1'
const QUESTION_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000
const COMPRESSED_AUDIO_FORMATS = new Set(['mp3', 'm4a', 'aac', 'ogg', 'opus', 'webm'])

export function cacheKeyForQuestions(levelId, gradeLevelId) {
  return `${QUESTION_CACHE_PREFIX}:${normalizeKeyPart(levelId)}:${normalizeKeyPart(gradeLevelId || 'all')}`
}

export function writeCachedQuestions(storage, levelId, gradeLevelId, questions = [], now = Date.now()) {
  if (!Array.isArray(questions) || questions.length === 0) return
  safeSet(storage, cacheKeyForQuestions(levelId, gradeLevelId), {
    version: 1,
    cachedAt: now,
    questions,
  })
}

export function readCachedQuestions(
  storage,
  levelId,
  gradeLevelId,
  now = Date.now(),
  ttlMs = QUESTION_CACHE_TTL_MS
) {
  const payload = safeGet(storage, cacheKeyForQuestions(levelId, gradeLevelId))
  if (!payload || !Array.isArray(payload.questions)) return null
  if (now - Number(payload.cachedAt || 0) > ttlMs) return null
  return payload.questions
}

export async function loadQuestionsWithOfflineCache({
  levelId,
  gradeLevelId,
  fetchQuestions,
  storage = getUniApi(),
  now = Date.now(),
  ttlMs = QUESTION_CACHE_TTL_MS,
}) {
  try {
    const questions = await fetchQuestions(levelId, gradeLevelId)
    if (Array.isArray(questions) && questions.length > 0) {
      writeCachedQuestions(storage, levelId, gradeLevelId, questions, now)
      return { questions, fromCache: false }
    }
  } catch {
    // The cached copy below is the offline fallback.
  }

  const cached = readCachedQuestions(storage, levelId, gradeLevelId, now, ttlMs)
  return {
    questions: cached || [],
    fromCache: !!cached,
  }
}

export function audioFormatOf(audioUrl = '') {
  const path = normalizeAudioUrl(audioUrl).split(/[?#]/)[0]
  const match = path.match(/\.([A-Za-z0-9]+)$/)
  return match ? match[1].toLowerCase() : ''
}

export function isCompressedAudioFormat(audioUrl = '') {
  return COMPRESSED_AUDIO_FORMATS.has(audioFormatOf(audioUrl))
}

export function readCachedAudioUrl(audioUrl, storage = getUniApi()) {
  const url = normalizeAudioUrl(audioUrl)
  if (!url) return ''
  const cache = safeGet(storage, AUDIO_CACHE_KEY) || {}
  const cached = cache[url]
  return typeof cached === 'string' ? cached : ''
}

export async function prefetchAudioFile(audioUrl, uniApi = getUniApi()) {
  const url = normalizeAudioUrl(audioUrl)
  if (!url) return ''

  const cached = readCachedAudioUrl(url, uniApi)
  if (cached) return cached

  if (!uniApi || typeof uniApi.downloadFile !== 'function') {
    return url
  }

  return new Promise((resolve) => {
    try {
      uniApi.downloadFile({
        url,
        success: (res) => {
          if (res?.statusCode && res.statusCode !== 200) {
            resolve(url)
            return
          }
          const tempFilePath = res?.tempFilePath
          if (!tempFilePath) {
            resolve(url)
            return
          }
          saveDownloadedAudio(url, tempFilePath, uniApi).then(resolve)
        },
        fail: () => resolve(url),
      })
    } catch {
      resolve(url)
    }
  })
}

export function normalizeAudioUrl(audioUrl = '') {
  return String(audioUrl || '').trim()
}

async function saveDownloadedAudio(url, tempFilePath, uniApi) {
  if (typeof uniApi.saveFile !== 'function') {
    writeCachedAudioUrl(url, tempFilePath, uniApi)
    return tempFilePath
  }

  return new Promise((resolve) => {
    try {
      uniApi.saveFile({
        tempFilePath,
        success: (res) => {
          const savedFilePath = res?.savedFilePath || tempFilePath
          writeCachedAudioUrl(url, savedFilePath, uniApi)
          resolve(savedFilePath)
        },
        fail: () => resolve(tempFilePath),
      })
    } catch {
      resolve(tempFilePath)
    }
  })
}

function writeCachedAudioUrl(remoteUrl, localUrl, storage) {
  const cache = safeGet(storage, AUDIO_CACHE_KEY) || {}
  safeSet(storage, AUDIO_CACHE_KEY, {
    ...cache,
    [normalizeAudioUrl(remoteUrl)]: localUrl,
  })
}

function safeGet(storage, key) {
  try {
    return storage?.getStorageSync ? storage.getStorageSync(key) : null
  } catch {
    return null
  }
}

function safeSet(storage, key, value) {
  try {
    if (storage?.setStorageSync) storage.setStorageSync(key, value)
  } catch {
    // Storage quota or platform restrictions should not block quiz flow.
  }
}

function normalizeKeyPart(value) {
  return encodeURIComponent(String(value || 'all'))
}

function getUniApi() {
  return typeof uni !== 'undefined' ? uni : null
}
