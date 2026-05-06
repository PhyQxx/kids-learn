import test from 'node:test'
import assert from 'node:assert/strict'
import {
  richContentToNodes,
  richContentToSpeechAudioUrl,
  richContentToSpeechText,
  richContentToText,
} from '../utils/richContent.mjs'

test('legacy plain text stays readable', () => {
  assert.equal(richContentToText('Plain question'), 'Plain question')
  assert.equal(richContentToNodes('Plain question')[0].children[0].text, 'Plain question')
})

test('rich JSON extracts paragraph text and image alt text', () => {
  const richJson = JSON.stringify({
    type: 'richText',
    version: 1,
    blocks: [
      { type: 'paragraph', text: 'Look' },
      { type: 'image', url: 'https://example.com/a.png', alt: 'Image alt' },
    ],
  })

  assert.equal(richContentToText(richJson), 'Look Image alt')
})

test('rich JSON exposes speech text and audio URL', () => {
  const richJson = JSON.stringify({
    type: 'richText',
    version: 1,
    speech: {
      text: 'Look and answer',
      audioUrl: 'https://example.com/q.mp3',
    },
    blocks: [
      { type: 'paragraph', text: 'Look' },
      { type: 'image', url: 'https://example.com/a.png', alt: 'Image alt' },
    ],
  })

  assert.equal(richContentToSpeechText(richJson), 'Look and answer')
  assert.equal(richContentToSpeechAudioUrl(richJson), 'https://example.com/q.mp3')
})

test('speech text ignores image alt when no speech text is configured', () => {
  const richJson = JSON.stringify({
    type: 'richText',
    version: 1,
    blocks: [
      { type: 'paragraph', text: 'Look' },
      { type: 'image', url: 'https://example.com/a.png', alt: 'Image alt' },
    ],
  })

  assert.equal(richContentToSpeechText(richJson), 'Look')
})

test('malformed JSON falls back to source text', () => {
  assert.equal(richContentToText('{bad json'), '{bad json')
})

test('image blocks become rich-text image nodes', () => {
  const richJson = JSON.stringify({
    type: 'richText',
    version: 1,
    blocks: [
      { type: 'image', url: 'https://example.com/a.png', alt: 'Image alt' },
    ],
  })

  const nodes = richContentToNodes(richJson)
  assert.equal(nodes[0].name, 'img')
  assert.equal(nodes[0].attrs.src, 'https://example.com/a.png')
})
