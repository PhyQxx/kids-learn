import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const appRoot = dirname(dirname(fileURLToPath(import.meta.url)))

function readAppFile(path) {
  return readFileSync(join(appRoot, path), 'utf8')
}

test('student navigation keeps parent mode out of the sidebar', () => {
  const mainPage = readAppFile('pages/main/index.vue')

  assert.equal(/key:\s*['"]parent['"]/.test(mainPage), false)
})

test('main page topbar keeps an explicit parent mode entry', () => {
  const mainPage = readAppFile('pages/main/index.vue')

  assert.match(mainPage, /家长模式/)
  assert.match(mainPage, /goParentMode/)
})

test('parent mode entry requires password verification', () => {
  const mainPage = readAppFile('pages/main/index.vue')
  const appLayout = readAppFile('components/AppLayout.vue')
  const authApi = readAppFile('api/auth.js')

  assert.match(mainPage, /ParentModePasswordGate/)
  assert.match(appLayout, /ParentModePasswordGate/)
  assert.match(mainPage, /parentModeGate\.value\?\.open\(\)/)
  assert.match(appLayout, /parentModeGate\.value\?\.open\(\)/)
  assert.match(authApi, /verifyPassword/)
  assert.match(authApi, /\/user\/verify-password/)
  assert.equal(/\/auth\/verify-password/.test(authApi), false)
  assert.match(mainPage, /ParentModePasswordGate/)
  assert.match(authApi, /verifyParentPin/)
  assert.match(authApi, /\/user\/parent-pin\/verify/)
  assert.equal(/function goParentMode\(\) \{[\s\S]*uni\.navigateTo\(\{ url: ['"]\/pages\/parent\/index['"] \}\)/.test(mainPage), false)
  assert.equal(/function goParentMode\(\) \{[\s\S]*uni\.navigateTo\(\{ url: ['"]\/pages\/parent\/index['"] \}\)/.test(appLayout), false)
})

test('parent mode has its own sidebar entries and student mode switch', () => {
  const appLayout = readAppFile('components/AppLayout.vue')
  const vipPage = readAppFile('pages/mine/vip.vue')

  assert.match(appLayout, /isParentMode/)
  assert.match(appLayout, /家长中心/)
  assert.match(appLayout, /会员中心/)
  assert.match(appLayout, /学生模式/)
  assert.match(vipPage, /<AppLayout/)
  assert.equal(/返回首页/.test(vipPage), false)
})

test('parent center is simplified for one account with two modes', () => {
  const parentPage = readAppFile('pages/parent/index.vue')

  assert.equal(/在线孩子/.test(parentPage), false)
  assert.equal(/家庭成员/.test(parentPage), false)
  assert.equal(/getFamily/.test(parentPage), false)
  assert.equal(/getRealtimeMonitor/.test(parentPage), false)
})

test('parent AI summary is generated only by manual refresh', () => {
  const parentPage = readAppFile('pages/parent/index.vue')
  const loadParentDataBody = parentPage.match(/async function loadParentData\(\) \{([\s\S]*?)\n\}/)?.[1] || ''

  assert.equal(/loadAiSummary\(/.test(loadParentDataBody), false)
  assert.match(parentPage, /@tap="loadAiSummary\(true\)"/)
})

test('vip center uses the same light page style as parent mode pages', () => {
  const vipPage = readAppFile('pages/mine/vip.vue')

  assert.match(vipPage, /theme="kids"/)
  assert.equal(/theme="dark"/.test(vipPage), false)
  assert.equal(/text-white/.test(vipPage), false)
  assert.match(vipPage, /\.plan-card[\s\S]*background:\s*#fff/)
})
