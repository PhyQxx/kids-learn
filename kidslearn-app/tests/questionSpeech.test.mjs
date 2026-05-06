import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveQuestionSpeech } from '../utils/questionSpeech.mjs'

test('direct speech fields are preferred for questions', () => {
  const speech = resolveQuestionSpeech({
    questionContent: 'Fallback content',
    questionSpeechText: 'Read me',
    questionAudioUrl: 'https://example.com/q.mp3',
  })

  assert.deepEqual(speech, {
    text: 'Read me',
    audioUrl: 'https://example.com/q.mp3',
  })
})

test('rich content speech metadata is used when direct fields are absent', () => {
  const speech = resolveQuestionSpeech({
    questionContent: JSON.stringify({
      type: 'richText',
      version: 1,
      speech: {
        text: 'Read rich question',
        audioUrl: 'https://example.com/rich.mp3',
      },
      blocks: [
        { type: 'paragraph', text: 'Visible rich question' },
      ],
    }),
  })

  assert.deepEqual(speech, {
    text: 'Read rich question',
    audioUrl: 'https://example.com/rich.mp3',
  })
})
