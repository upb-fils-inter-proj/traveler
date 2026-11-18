import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { SECTIONS } from '../sections.ts'

type SectionTitleKey = (typeof SECTIONS)[number]['titleKey']

/** A section of the app that has no content yet. */
export default function SectionPage({ titleKey }: { titleKey: SectionTitleKey }) {
  const { t } = useTranslation()
  const title = t(titleKey)
  const appName = t('app.name')

  useEffect(() => {
    document.title = `${title} - ${appName}`
  }, [title, appName])

  return (
    <>
      <h1>{title}</h1>
      <p className="placeholder">{t('sections.empty')}</p>
    </>
  )
}
