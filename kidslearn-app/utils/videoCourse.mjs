export function normalizeVideoCourses(rows = []) {
  return (Array.isArray(rows) ? rows : []).map((row) => {
    const durationSeconds = toNonNegativeInt(row.durationSeconds)
    const progressSeconds = Math.min(toNonNegativeInt(row.progressSeconds), durationSeconds || Number.MAX_SAFE_INTEGER)
    const progressPercent = percent(progressSeconds, durationSeconds)
    return {
      id: row.id,
      title: firstText(row.title, row.videoTitle, row.name, '视频课程'),
      description: firstText(row.description, row.videoDesc, ''),
      videoUrl: firstText(row.videoUrl, ''),
      coverUrl: firstText(row.coverUrl, ''),
      courseLevelId: row.courseLevelId || null,
      durationSeconds,
      durationText: formatVideoDuration(durationSeconds),
      isFree: row.isFree !== 0,
      progressSeconds,
      progressPercent,
      completed: Boolean(row.completed) || progressPercent >= 90,
      locked: Boolean(row.locked),
    }
  })
}

export function updateVideoProgress(video, currentSeconds, durationSeconds = video.durationSeconds) {
  const duration = Math.max(toNonNegativeInt(durationSeconds), toNonNegativeInt(video.durationSeconds))
  const progressSeconds = Math.max(toNonNegativeInt(video.progressSeconds), toNonNegativeInt(currentSeconds))
  const clampedProgress = Math.min(progressSeconds, duration || progressSeconds)
  const progressPercent = percent(clampedProgress, duration)
  return {
    ...video,
    durationSeconds: duration,
    durationText: formatVideoDuration(duration),
    progressSeconds: clampedProgress,
    progressPercent,
    completed: Boolean(video.completed) || progressPercent >= 90,
  }
}

export function formatVideoDuration(seconds) {
  const total = toNonNegativeInt(seconds)
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const secs = total % 60
  if (hours > 0) {
    return [hours, minutes, secs].map(padTime).join(':')
  }
  return [minutes, secs].map(padTime).join(':')
}

function percent(value, total) {
  if (!total) return 0
  return Math.min(100, Math.round(value * 100 / total))
}

function toNonNegativeInt(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number) || number < 0) return 0
  return Math.floor(number)
}

function firstText(...values) {
  for (const value of values) {
    const text = String(value || '').trim()
    if (text) return text
  }
  return ''
}

function padTime(value) {
  return String(value).padStart(2, '0')
}
