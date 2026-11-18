import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './locales/en.json'
import ro from './locales/ro.json'

/** Supported languages, in the order the language switcher lists them. */
export const LANGUAGES = ['ro', 'en'] as const
export type Language = (typeof LANGUAGES)[number]

/** Used when the browser language is neither Romanian nor English. */
export const DEFAULT_LANGUAGE: Language = 'en'

/** Each language is named in itself, so it stays readable in any language. */
export const LANGUAGE_NAMES: Record<Language, string> = {
  ro: 'Română',
  en: 'English',
}

export const LANGUAGE_STORAGE_KEY = 'traveler-admin.language'

export function isLanguage(value: unknown): value is Language {
  return LANGUAGES.some((language) => language === value)
}

function readStoredLanguage(): Language | null {
  try {
    const stored = window.localStorage.getItem(LANGUAGE_STORAGE_KEY)
    return isLanguage(stored) ? stored : null
  } catch {
    // Storage can be unavailable (private mode, blocked cookies).
    return null
  }
}

/**
 * The language the user chose before, otherwise the browser language when it
 * is Romanian or English, otherwise English.
 */
export function detectLanguage(): Language {
  const stored = readStoredLanguage()
  if (stored !== null) return stored

  const browserLanguage = window.navigator.language.toLowerCase().split('-')[0]
  return isLanguage(browserLanguage) ? browserLanguage : DEFAULT_LANGUAGE
}

/** Switches the language and remembers the choice for the next visit. */
export async function setLanguage(language: Language): Promise<void> {
  try {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, language)
  } catch {
    // The language still changes for this visit.
  }
  await i18n.changeLanguage(language)
}

i18n.on('languageChanged', (language) => {
  document.documentElement.lang = language
})

void i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    ro: { translation: ro },
  },
  lng: detectLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  supportedLngs: LANGUAGES,
  interpolation: {
    // React already escapes what it renders.
    escapeValue: false,
  },
  // The resources are bundled, so there is nothing to wait for.
  initAsync: false,
  react: { useSuspense: false },
})

export default i18n
