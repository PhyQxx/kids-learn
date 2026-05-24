import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildMatchAnswer,
  buildOrderAnswer,
  normalizeQuizQuestion,
  normalizeSpeechAttempt
} from '../utils/questionInteraction.mjs'

test('normalizes order questions and builds ordered answer from original labels', () => {
  const question = normalizeQuizQuestion({
    id: 1,
    questionType: 4,
    questionContent: '把词语排成句子',
    options: [
      { optionLabel: 'A', answerValue: 'C', optionContent: '学习', optionText: '学习' },
      { optionLabel: 'B', answerValue: 'A', optionContent: '我', optionText: '我' },
      { optionLabel: 'C', answerValue: 'B', optionContent: '爱', optionText: '爱' }
    ]
  })

  assert.equal(question.interactionType, 'order')
  assert.equal(buildOrderAnswer([question.options[1], question.options[2], question.options[0]]), 'A,B,C')
})

test('normalizes match questions from json pair option content', () => {
  const question = normalizeQuizQuestion({
    id: 2,
    questionType: 5,
    questionContent: '连一连',
    options: [
      { optionLabel: 'A', answerValue: 'A', optionContent: '{"left":"大","right":"小"}' },
      { optionLabel: 'B', answerValue: 'B', optionContent: '{"left":"上","right":"下"}' }
    ]
  })

  assert.equal(question.interactionType, 'match')
  assert.equal(question.options[0].pairLeft, '大')
  assert.equal(question.options[0].pairRight, '小')
  assert.equal(buildMatchAnswer({ B: 'B', A: 'A' }), 'A=A|B=B')
})

test('normalizes voice attempts for tolerant comparison', () => {
  assert.equal(normalizeQuizQuestion({ questionType: 6, options: [] }).interactionType, 'voice')
  assert.equal(normalizeSpeechAttempt(' I like apples! '), 'ilikeapples')
})
