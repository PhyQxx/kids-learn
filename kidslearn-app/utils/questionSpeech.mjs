import { richContentToSpeechAudioUrl, richContentToSpeechText } from './richContent.mjs'

export function resolveQuestionSpeech(question = {}) {
  const questionContent = question.questionContent || ''
  return {
    text:
      firstText(question.questionSpeechText, question.speechText, question.plainText, question.text)
      || richContentToSpeechText(questionContent),
    audioUrl:
      firstText(question.questionAudioUrl, question.audioUrl)
      || richContentToSpeechAudioUrl(questionContent),
  }
}

function firstText(...values) {
  for (const value of values) {
    const text = String(value || '').trim()
    if (text) {
      return text
    }
  }
  return ''
}
