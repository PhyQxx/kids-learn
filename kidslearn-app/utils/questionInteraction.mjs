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
  let options
  if (questionType === 2) {
    // 判断题始终使用"正确"/"错误"，忽略数据库中可能错误的 A/B/C/D 选项
    options = [
      { label: '正确', answerValue: '正确', text: '正确', speechText: '', audioUrl: '', nodes: '正确', pairLeft: '正确', pairRight: '正确', correct: false, isCorrect: null },
      { label: '错误', answerValue: '错误', text: '错误', speechText: '', audioUrl: '', nodes: '错误', pairLeft: '错误', pairRight: '错误', correct: false, isCorrect: null }
    ]
  } else {
    options = Array.isArray(raw.options) ? raw.options.map((option, index) => normalizeOption(option, index, questionType)) : []
  }
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

  // 对于判断题(2)，optionLabel 是"正确"/"错误"（后端已处理）
  // 对于连线题(5)，optionLabel 是左边，optionContent 是右边
  // 对于选择题(1)，optionLabel 是 A/B/C/D

  let label = option.optionLabel || ''
  if ((questionType === 1 || questionType === 4) && !label) label = fallbackLabel

  const pair = questionType === 5 ? parseMatchPair(option.optionContent, label) : null
  const text = option.optionText || pair?.right || richContentToText(option.optionContent) || ''

  // answerValue 决定了前端选中该项时提交的值
  let answerValue
  if (questionType === 5) {
    answerValue = label
  } else if (option.answerValue) {
    answerValue = option.answerValue
  } else if (questionType === 4) {
    answerValue = text || label
  } else {
    answerValue = label || text
  }

  // 连线题：处理左右内容显示
  let pairLeft, pairRight
  if (questionType === 5) {
    // 检查 label 是否是 A/B/C 格式（单个大写字母）
    const isLabelABC = /^[A-Z]$/.test(label)

    if (pair) {
      // JSON 格式：使用解析后的值
      pairLeft = pair.left || label
      pairRight = pair.right || text
    } else if (isLabelABC && text) {
      // 纯文本格式且 label 是 A/B/C：optionContent 是右侧内容
      // 左侧显示 A/B/C，右侧显示 optionContent
      pairLeft = label
      pairRight = text
    } else {
      // 其他情况：label 是左侧内容，text 是右侧内容
      pairLeft = label
      pairRight = text
    }
  } else {
    pairLeft = pair?.left || label || text
    pairRight = pair?.right || text
  }

  return {
    label,
    answerValue,
    text,
    speechText: option.optionSpeechText || '',
    audioUrl: option.optionAudioUrl || '',
    nodes: richContentToNodes(option.optionContent),
    pairLeft,
    pairRight,
    correct: false, // will be updated by submit answer
    isCorrect: option.isCorrect // from backend, usually hidden, but might be present in practice
  }
}

function parseMatchPair(value, label) {
  if (!value) return null

  try {
    const parsed = JSON.parse(value)
    if (parsed && typeof parsed === 'object') {
      // 支持 {"left":"xxx","right":"xxx"} 格式
      // 也支持 {"right":"xxx"} 格式，left 使用 label
      if (parsed.left !== undefined || parsed.right !== undefined) {
        return {
          left: parsed.left ? String(parsed.left) : (label || ''),
          right: parsed.right ? String(parsed.right) : ''
        }
      }
      // 支持 {"text":"xxx"} 格式，left 使用 label
      if (parsed.text) {
        return {
          left: label || '',
          right: String(parsed.text)
        }
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
