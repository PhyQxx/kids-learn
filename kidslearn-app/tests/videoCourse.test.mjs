import assert from 'node:assert/strict'
import test from 'node:test'
import {
  formatVideoDuration,
  normalizeVideoCourses,
  updateVideoProgress,
} from '../utils/videoCourse.mjs'

test('normalizes video course rows with progress and duration text', () => {
  const videos = normalizeVideoCourses([
    {
      id: 1,
      title: '认识声母',
      description: '10 分钟学会 b p m f',
      videoUrl: 'https://cdn.example.com/lesson.mp4',
      coverUrl: 'https://cdn.example.com/cover.png',
      courseLevelId: 8,
      durationSeconds: 615,
      isFree: 1,
      progressSeconds: 123,
      completed: false,
    },
  ])

  assert.deepEqual(videos[0], {
    id: 1,
    title: '认识声母',
    description: '10 分钟学会 b p m f',
    videoUrl: 'https://cdn.example.com/lesson.mp4',
    coverUrl: 'https://cdn.example.com/cover.png',
    courseLevelId: 8,
    durationSeconds: 615,
    durationText: '10:15',
    isFree: true,
    progressSeconds: 123,
    progressPercent: 20,
    completed: false,
    locked: false,
  })
})

test('formats long and short video durations', () => {
  assert.equal(formatVideoDuration(0), '00:00')
  assert.equal(formatVideoDuration(75), '01:15')
  assert.equal(formatVideoDuration(3661), '01:01:01')
})

test('updates video progress without moving backwards', () => {
  const video = normalizeVideoCourses([
    { id: 2, durationSeconds: 100, progressSeconds: 60 },
  ])[0]

  assert.equal(updateVideoProgress(video, 40, 100).progressSeconds, 60)
  assert.deepEqual(updateVideoProgress(video, 92, 100), {
    ...video,
    durationSeconds: 100,
    durationText: '01:40',
    progressSeconds: 92,
    progressPercent: 92,
    completed: true,
  })
})
