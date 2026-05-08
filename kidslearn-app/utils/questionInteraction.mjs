import { richContentToNodes, richContentToText } from './richContent.mjs'

export const QUESTION_INTERACTIONS = {
  1: 'single',
  2: 'order',
  3: 'match',
  4: 'voice'
}

export function normalizeQuizQuestion(raw = {}) {
  const questionType = Number(raw.questionType || 1)
  const interactionType = QUESTION_INTERACTIONS[questionType] || 'single'
  const options = Array.isArray(raw.options) ? raw.options.map((option, index) => normalizeOption(option, index)) : []
  const voiceText = interactionType === 'voice'
    ? (options[0]?.text || raw.questionSpeechText || raw.questionText || richContentToText(raw.questionContent))
    : ''

  return {
    id: raw.id,
    questionType,
    interactionType,
    emoji: interactionEmoji(interactionType),
    questionContent: raw.questionContent,
    text: raw.questionText || richContentToText(raw.questionContent),
    plainText: raw.questionText || richContentToText(raw.questionContent),
    questionSpeechText: raw.questionSpeechText || '',
    questionAudioUrl: raw.questionAudioUrl || '',
    nodes: richContentToNodes(raw.questionContent),
    score: raw.score || 10,
    options,
    voiceText
  }
}

export function buildOrderAnswer(items = []) {
  return items.map(item => item.answerValue || item.label).join(',')
}

export function buildMatchAnswer(pairs = {}) {
  return Object.keys(pairs)
    .sort()
    .map(left => `${left}=${pairs[left]}`)
    .join('|')
}

export function normalizeSpeechAttempt(value = '') {
  return String(value)
    .trim()
    .toLowerCase()
    .replace(/[\s.,!?;:'"“”‘’。！？；：，、-]/g, '')
}

function normalizeOption(option = {}, index) {
  const fallbackLabel = String.fromCharCode(65 + index)
  const label = option.optionLabel || fallbackLabel
  const answerValue = option.answerValue || label
  const pair = parsePairContent(option.optionContent)
  const text = option.optionText || pair.text || richContentToText(option.optionContent)

  return {
    label,
    answerValue,
    text,
    speechText: option.optionSpeechText || '',
    audioUrl: option.optionAudioUrl || '',
    nodes: richContentToNodes(option.optionContent),
    pairLeft: pair.left || text,
    pairRight: pair.right || text,
    correct: false
  }
}

function parsePairContent(value) {
  if (!value) return {}
  try {
    const parsed = JSON.parse(value)
    if (parsed && typeof parsed === 'object') {
      return {
        left: parsed.left ? String(parsed.left) : '',
        right: parsed.right ? String(parsed.right) : '',
        text: parsed.text ? String(parsed.text) : ''
      }
    }
  } catch {
    // Plain option content.
  }
  return {}
}

function interactionEmoji(interactionType) {
  if (interactionType === 'order') return '🧩'
  if (interactionType === 'match') return '🔗'
  if (interactionType === 'voice') return '🎙️'
  return '❓'
}
