const EMPTY_SUMMARY = {
  summary: '',
  highlights: [],
  concerns: [],
  suggestions: []
}

function toTextList(value) {
  if (!Array.isArray(value)) return []
  return value
    .filter(item => item !== null && item !== undefined)
    .map(item => String(item).trim())
    .filter(Boolean)
}

export function normalizeParentAiSummary(raw = {}) {
  const source = raw?.data && typeof raw.data === 'object' ? raw.data : raw
  if (!source || typeof source !== 'object') {
    return { ...EMPTY_SUMMARY }
  }

  return {
    summary: String(source.summary || '').trim(),
    highlights: toTextList(source.highlights),
    concerns: toTextList(source.concerns),
    suggestions: toTextList(source.suggestions)
  }
}
