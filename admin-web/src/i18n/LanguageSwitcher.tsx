import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { isLanguage, LANGUAGE_NAMES, LANGUAGES, setLanguage } from './index.ts'

export default function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  const id = useId()

  return (
    <div className="language-switcher">
      <label htmlFor={id}>{t('language.label')}</label>
      <select
        id={id}
        value={i18n.resolvedLanguage}
        onChange={(event) => {
          const language = event.target.value
          if (isLanguage(language)) void setLanguage(language)
        }}
      >
        {LANGUAGES.map((language) => (
          <option key={language} value={language}>
            {LANGUAGE_NAMES[language]}
          </option>
        ))}
      </select>
    </div>
  )
}
