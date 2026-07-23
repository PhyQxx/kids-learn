import test from 'node:test'
import assert from 'node:assert/strict'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import compilerSfc from '../node_modules/@vue/compiler-sfc/dist/compiler-sfc.cjs.js'

const appRoot = dirname(dirname(fileURLToPath(import.meta.url)))
const { parse, compileScript, compileStyleAsync } = compilerSfc

const vueFiles = collectVueFiles(appRoot)
  .filter(file => !file.includes('node_modules'))
  .filter(file => !file.includes('unpackage'))

test('source vue files compile', () => {
  const failures = []

  for (const filename of vueFiles) {
    const source = readFileSync(filename, 'utf8')
    const sfc = parse(source, { filename })
    if (sfc.errors.length > 0) {
      failures.push(`${formatFilename(filename)}: ${sfc.errors.map(error => error.message || String(error)).join('; ')}`)
      continue
    }
    try {
      compileScript(sfc.descriptor, { id: filename })
    } catch (error) {
      failures.push(`${formatFilename(filename)}: ${error.message}`)
    }
  }

  assert.deepEqual(failures, [])
})

function collectVueFiles(dir) {
  const files = []
  for (const entry of readdirSync(dir)) {
    const fullPath = join(dir, entry)
    const stat = statSync(fullPath)
    if (stat.isDirectory()) {
      files.push(...collectVueFiles(fullPath))
    } else if (fullPath.endsWith('.vue')) {
      files.push(fullPath.replaceAll('\\', '/'))
    }
  }
  return files
}

function formatFilename(filename) {
  return relative(appRoot, filename).replaceAll('\\', '/')
}

test('login register page script compiles', () => {
  const filename = join(appRoot, 'pages/login/register.vue')
  const source = readFileSync(filename, 'utf8')
  const sfc = parse(source, { filename })

  assert.deepEqual(sfc.errors, [])
  assert.doesNotThrow(() => compileScript(sfc.descriptor, { id: filename }))
})

test('source vue scss styles compile', async () => {
  const failures = []

  for (const filename of vueFiles) {
    const source = readFileSync(filename, 'utf8')
    const sfc = parse(source, { filename })
    if (sfc.errors.length > 0) continue

    for (const [index, style] of sfc.descriptor.styles.entries()) {
      if (style.lang !== 'scss') continue

      const result = await compileStyleAsync({
        filename,
        id: `${filename}-${index}`,
        source: style.content,
        scoped: style.scoped,
        preprocessLang: style.lang,
        preprocessOptions: {
          importer: [
            url => url.startsWith('@/') ? { file: resolve(appRoot, url.slice(2)) } : null
          ]
        }
      })

      if (result.errors.length > 0) {
        failures.push(`${formatFilename(filename)}: ${result.errors.map(error => error.message || String(error)).join('; ')}`)
      }
    }
  }

  assert.deepEqual(failures, [])
})
