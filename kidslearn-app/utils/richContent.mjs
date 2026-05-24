const IMAGE_PLACEHOLDER = '[image]'

export function parseRichContent(value) {
  if (!value) {
    return createRichContent([{ type: 'paragraph', text: '' }])
  }

  try {
    const parsed = JSON.parse(value)
    if (parsed?.type === 'richText' && Array.isArray(parsed.blocks)) {
      const blocks = parsed.blocks.map(normalizeBlock).filter(Boolean)
      return createRichContent(
        blocks.length > 0 ? blocks : [{ type: 'paragraph', text: '' }],
        normalizeSpeech(parsed.speech)
      )
    }
  } catch {
    // Legacy plain text.
  }

  return createRichContent([{ type: 'paragraph', text: String(value) }])
}

export function richContentToText(value) {
  return parseRichContent(value).blocks
    .map((block) => {
      if (block.type === 'paragraph') return block.text.trim()
      return block.alt?.trim() || IMAGE_PLACEHOLDER
    })
    .filter(Boolean)
    .join(' ')
}

export function richContentToSpeechText(value) {
  const content = parseRichContent(value)
  const speechText = content.speech?.text?.trim()
  if (speechText) {
    return speechText
  }

  return content.blocks
    .map((block) => {
      if (block.type === 'paragraph') return block.text.trim()
      return ''
    })
    .filter(Boolean)
    .join(' ')
}

export function richContentToSpeechAudioUrl(value) {
  return parseRichContent(value).speech?.audioUrl?.trim() || ''
}

export function richContentToNodes(value) {
  const nodes = parseRichContent(value).blocks.map((block) => {
    if (block.type === 'image') {
      return {
        name: 'img',
        attrs: {
          src: block.url,
          alt: block.alt || '',
          style: 'max-width:100%;height:auto;border-radius:12px;display:block;margin:8px auto;',
        },
      }
    }

    return {
      name: 'p',
      attrs: {
        style: 'margin:0;line-height:1.45;',
      },
      children: [
        {
          type: 'text',
          text: block.text,
        },
      ],
    }
  })

  return nodes.length > 0 ? nodes : richContentToNodes('')
}

function createRichContent(blocks, speech = {}) {
  return {
    type: 'richText',
    version: 1,
    blocks,
    speech,
  }
}

function normalizeSpeech(speech) {
  if (!speech || typeof speech !== 'object') {
    return {}
  }
  return {
    text: speech.text ? String(speech.text) : '',
    audioUrl: speech.audioUrl ? String(speech.audioUrl) : '',
  }
}

function normalizeBlock(block) {
  if (!block || typeof block !== 'object') {
    return null
  }
  if (block.type === 'paragraph') {
    return {
      type: 'paragraph',
      text: String(block.text ?? ''),
    }
  }
  if (block.type === 'image' && block.url) {
    return {
      type: 'image',
      url: String(block.url),
      alt: block.alt ? String(block.alt) : '',
    }
  }
  return null
}
