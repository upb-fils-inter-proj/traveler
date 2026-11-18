import 'i18next'
import type en from './locales/en.json'

// Makes t('...') only accept keys that exist in the English file.
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation'
    resources: { translation: typeof en }
  }
}
