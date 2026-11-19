import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  detectLanguage,
  LANGUAGE_STORAGE_KEY,
  LANGUAGES,
  setLanguage,
} from './index.ts'
import en from './locales/en.json'
import ro from './locales/ro.json'

function browserLanguage(language: string) {
  vi.spyOn(window.navigator, 'language', 'get').mockReturnValue(language)
}

function flatten(value: unknown, prefix = ''): Record<string, string> {
  if (typeof value === 'string') return { [prefix]: value }
  if (typeof value !== 'object' || value === null) return {}
  return Object.entries(value).reduce<Record<string, string>>(
    (all, [key, child]) => ({
      ...all,
      ...flatten(child, prefix === '' ? key : `${prefix}.${key}`),
    }),
    {},
  )
}

describe('detectLanguage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it.each([
    ['ro', 'ro'],
    ['ro-RO', 'ro'],
    ['RO-ro', 'ro'],
    ['en', 'en'],
    ['en-GB', 'en'],
  ])('follows the browser language %s', (browser, expected) => {
    browserLanguage(browser)
    expect(detectLanguage()).toBe(expected)
  })

  it.each(['de-DE', 'fr', 'hu-HU', ''])(
    'falls back to English for the browser language "%s"',
    (browser) => {
      browserLanguage(browser)
      expect(detectLanguage()).toBe('en')
    },
  )

  it('prefers the language the user chose before', () => {
    browserLanguage('en-US')
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'ro')
    expect(detectLanguage()).toBe('ro')
  })

  it('ignores a stored language that is not supported', () => {
    browserLanguage('ro-RO')
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'de')
    expect(detectLanguage()).toBe('ro')
  })

  it('still works when storage is not available', async () => {
    browserLanguage('ro-RO')
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError')
    })

    expect(detectLanguage()).toBe('ro')
    await expect(setLanguage('en')).resolves.toBeUndefined()
  })
})

describe('setLanguage', () => {
  it('remembers the choice and updates the html lang attribute', async () => {
    await setLanguage('ro')

    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('ro')
    expect(document.documentElement.lang).toBe('ro')
  })
})

describe('translations', () => {
  const english = flatten(en)
  const romanian = flatten(ro)

  it('offer Romanian and English', () => {
    expect([...LANGUAGES].sort()).toEqual(['en', 'ro'])
  })

  it('have the same keys in both languages', () => {
    expect(Object.keys(romanian).sort()).toEqual(Object.keys(english).sort())
  })

  it.each(['access.', 'api.', 'attractions.'])(
    'translate the texts of the attraction screens under %s',
    (prefix) => {
      const keys = (texts: Record<string, string>) =>
        Object.keys(texts).filter((key) => key.startsWith(prefix))
      expect(keys(english).length).toBeGreaterThan(0)
      expect(keys(romanian).sort()).toEqual(keys(english).sort())
    },
  )

  it('have no empty text', () => {
    for (const text of [...Object.values(english), ...Object.values(romanian)]) {
      expect(text.trim()).not.toBe('')
    }
  })

  it('use the same placeholders in both languages', () => {
    const placeholders = (text: string) => text.match(/{{\w+}}/g)?.sort() ?? []
    for (const [key, text] of Object.entries(english)) {
      expect(placeholders(romanian[key] ?? ''), key).toEqual(placeholders(text))
    }
  })

  it('write Romanian with comma-below diacritics, not cedillas', () => {
    for (const text of Object.values(romanian)) {
      expect(text).not.toMatch(/[ŞşŢţ]/)
    }
  })
})
