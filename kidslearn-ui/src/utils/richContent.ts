export type RichParagraphBlock = {
  type: 'paragraph'
  text: string
}

export type RichImageBlock = {
  type: 'image'
  url: string
  alt?: string
}

export type RichBlock = RichParagraphBlock | RichImageBlock

export type RichSpeech = {
  text?: string
  audioUrl?: string
}

export type RichContent = {
  type: 'richText'
  version: 1
  speech?: RichSpeech
  blocks: RichBlock[]
}

export function parseRichContent(value?: string | null): RichContent {
  if (!value) {
    return createRichContent([{ type: 'paragraph', text: '' }])
  }

  try {
    const parsed = JSON.parse(value)
    if (parsed?.type === 'richText' && Array.isArray(parsed.blocks)) {
      const blocks = parsed.blocks
        .map(normalizeBlock)
        .filter((block: RichBlock | null): block is RichBlock => block !== null)
      return createRichContent(
        blocks.length > 0 ? blocks : [{ type: 'paragraph', text: '' }],
        normalizeSpeech(parsed.speech)
      )
    }
  } catch {
    // Legacy plain text.
  }

  return createRichContent([{ type: 'paragraph', text: value }])
}

export function serializeRichContent(blocks: RichBlock[], speech?: RichSpeech): string {
  return JSON.stringify(createRichContent(normalizeBlocks(blocks), normalizeSpeech(speech)))
}

export function richContentToText(value?: string | null): string {
  return parseRichContent(value).blocks
    .map((block) => {
      if (block.type === 'paragraph') return block.text.trim()
      return block.alt?.trim() || '[image]'
    })
    .filter(Boolean)
    .join(' ')
}

export function richContentToSpeechText(value?: string | null): string {
  const content = parseRichContent(value)
  const speechText = content.speech?.text?.trim()
  if (speechText) return speechText

  return content.blocks
    .map((block) => {
      if (block.type === 'paragraph') return block.text.trim()
      return block.alt?.trim() || ''
    })
    .filter(Boolean)
    .join(' ')
}

export function richContentSpeech(value?: string | null): Required<RichSpeech> {
  const speech = parseRichContent(value).speech || {}
  return {
    text: speech.text?.trim() || '',
    audioUrl: speech.audioUrl?.trim() || '',
  }
}

export function withRichContentSpeech(value: string | undefined | null, speech: RichSpeech): string {
  const content = parseRichContent(value)
  return serializeRichContent(content.blocks, speech)
}

export function richContentSummary(value?: string | null, maxLength = 40): string {
  const text = richContentToText(value)
  if (text.length <= maxLength) return text
  if (maxLength <= 3) return text.slice(0, maxLength)
  return `${text.slice(0, maxLength - 3)}...`
}

function createRichContent(blocks: RichBlock[], speech?: RichSpeech): RichContent {
  const content: RichContent = {
    type: 'richText',
    version: 1,
    blocks,
  }
  const normalizedSpeech = normalizeSpeech(speech)
  if (normalizedSpeech.text || normalizedSpeech.audioUrl) {
    content.speech = normalizedSpeech
  }
  return content
}

function normalizeBlocks(blocks: RichBlock[]): RichBlock[] {
  const normalized = blocks
    .map(normalizeBlock)
    .filter((block: RichBlock | null): block is RichBlock => block !== null)
  return normalized.length > 0 ? normalized : [{ type: 'paragraph', text: '' }]
}

function normalizeBlock(block: any): RichBlock | null {
  if (!block || typeof block !== 'object') {
    return null
  }
  if (block.type === 'paragraph') {
    return { type: 'paragraph', text: String(block.text ?? '') }
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

function normalizeSpeech(speech: any): RichSpeech {
  if (!speech || typeof speech !== 'object') {
    return {}
  }
  return {
    text: speech.text ? String(speech.text) : '',
    audioUrl: speech.audioUrl ? String(speech.audioUrl) : '',
  }
}
