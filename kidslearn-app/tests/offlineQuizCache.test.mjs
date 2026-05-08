import assert from 'node:assert/strict'
import test from 'node:test'
import {
  audioFormatOf,
  cacheKeyForQuestions,
  isCompressedAudioFormat,
  loadQuestionsWithOfflineCache,
  prefetchAudioFile,
  readCachedAudioUrl,
  readCachedQuestions,
  writeCachedQuestions,
} from '../utils/offlineQuizCache.mjs'

function createStorage() {
  const values = new Map()
  return {
    getStorageSync(key) {
      return values.get(key)
    },
    setStorageSync(key, value) {
      values.set(key, value)
    },
    removeStorageSync(key) {
      values.delete(key)
    },
  }
}

test('builds stable question cache keys by level and grade', () => {
  assert.equal(cacheKeyForQuestions(12, 3), 'kidslearn:quiz-questions:v1:12:3')
  assert.equal(cacheKeyForQuestions(12, null), 'kidslearn:quiz-questions:v1:12:all')
})

test('reads cached questions only while the cache is fresh', () => {
  const storage = createStorage()
  writeCachedQuestions(storage, 12, 3, [{ id: 1 }], 1_000)

  assert.deepEqual(readCachedQuestions(storage, 12, 3, 1_000 + 60_000), [{ id: 1 }])
  assert.equal(readCachedQuestions(storage, 12, 3, 1_000 + 8 * 24 * 60 * 60 * 1000), null)
})

test('falls back to cached questions when the network request fails', async () => {
  const storage = createStorage()
  writeCachedQuestions(storage, 12, 'g2', [{ id: 9 }], 2_000)

  const result = await loadQuestionsWithOfflineCache({
    levelId: 12,
    gradeLevelId: 'g2',
    storage,
    now: 3_000,
    fetchQuestions: async () => {
      throw new Error('offline')
    },
  })

  assert.equal(result.fromCache, true)
  assert.deepEqual(result.questions, [{ id: 9 }])
})

test('fresh network questions replace the offline cache', async () => {
  const storage = createStorage()

  const result = await loadQuestionsWithOfflineCache({
    levelId: 12,
    gradeLevelId: 3,
    storage,
    now: 3_000,
    fetchQuestions: async () => [{ id: 2 }],
  })

  assert.equal(result.fromCache, false)
  assert.deepEqual(result.questions, [{ id: 2 }])
  assert.deepEqual(readCachedQuestions(storage, 12, 3, 3_000), [{ id: 2 }])
})

test('detects audio formats from URLs and query strings', () => {
  assert.equal(audioFormatOf('https://cdn.example.com/q-1.mp3?token=abc'), 'mp3')
  assert.equal(audioFormatOf('/audio/q-2.WAV#v1'), 'wav')
  assert.equal(isCompressedAudioFormat('https://cdn.example.com/q-1.m4a'), true)
  assert.equal(isCompressedAudioFormat('https://cdn.example.com/q-1.wav'), false)
})

test('prefetches remote audio and stores a saved local path', async () => {
  const storage = createStorage()
  const uniApi = {
    ...storage,
    downloadFile({ success }) {
      success({ statusCode: 200, tempFilePath: '/tmp/q-1.wav' })
    },
    saveFile({ tempFilePath, success }) {
      success({ savedFilePath: `/saved${tempFilePath}` })
    },
  }

  const localPath = await prefetchAudioFile('https://cdn.example.com/q-1.wav', uniApi)

  assert.equal(localPath, '/saved/tmp/q-1.wav')
  assert.equal(readCachedAudioUrl('https://cdn.example.com/q-1.wav', storage), '/saved/tmp/q-1.wav')
})
