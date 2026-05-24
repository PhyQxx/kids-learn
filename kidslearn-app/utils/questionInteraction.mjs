import { richContentToNodes, richContentToText } from './richContent.mjs'

export const QUESTION_INTERACTIONS = {
  1: 'single', // 选择题
  2: 'single', // 判断题 (展示形式与单选类似)
  3: 'fill',   // 填空题
  4: 'order',  // 排序题
  5: 'match',  // 连线题
  6: 'voice'   // 语音题 (预留)
}

export function normalizeQuizQuestion(raw = {}) {
  const questionType = Number(raw.questionType || 1)
  const interactionType = QUESTION_INTERACTIONS[questionType] || 'single'
  const options = Array.isArray(raw.options) ? raw.options.map((option, index) => normalizeOption(option, index, questionType)) : []
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
  // 后端比对的是 optionLabel 或者 optionContent 的逗号拼接
  return items.map(item => item.answerValue || (item.label && item.label !== '_' ? item.label : item.text)).join(',')
}

export function buildMatchAnswer(pairs = {}) {
  // 后端比对的是 left=right|left2=right2
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

function normalizeOption(option = {}, index, questionType) {
  const fallbackLabel = String.fromCharCode(65 + index)
  
  // 对于判断题(2)，可能没有 optionLabel，后端判断用的是 optionContent
  // 对于连线题(5)，optionLabel 是左边，optionContent 是右边
  
  let label = option.optionLabel || ''
  if ((questionType === 1 || questionType === 4 || questionType === 5) && !label) label = fallbackLabel
  
  const pair = questionType === 5 ? parseMatchPair(option.optionContent) : null
  const text = option.optionText || pair?.right || richContentToText(option.optionContent) || ''
  
  // answerValue 决定了前端选中该项时提交的值
  const answerValue = questionType === 5 ? label : (option.answerValue || label || text)

  return {
    label,
    answerValue,
    text,
    speechText: option.optionSpeechText || '',
    audioUrl: option.optionAudioUrl || '',
    nodes: richContentToNodes(option.optionContent),
    pairLeft: pair?.left || label || text,
    pairRight: pair?.right || text,
    correct: false, // will be updated by submit answer
    isCorrect: option.isCorrect // from backend, usually hidden, but might be present in practice
  }
}

function parseMatchPair(value) {
  if (!value) return null

  try {
    const parsed = JSON.parse(value)
    if (parsed && typeof parsed === 'object') {
      return {
        left: parsed.left ? String(parsed.left) : '',
        right: parsed.right ? String(parsed.right) : ''
      }
    }
  } catch {
    return null
  }

  return null
}

function interactionEmoji(interactionType) {
  if (interactionType === 'order') return '🧩'
  if (interactionType === 'match') return '🔗'
  if (interactionType === 'fill') return '✍️'
  if (interactionType === 'voice') return '🎙️'
  return '❓'
}
