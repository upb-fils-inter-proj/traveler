import { type ReactNode, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { SECTIONS } from '../sections.ts'

type SectionTitleKey = (typeof SECTIONS)[number]['titleKey']

interface SectionPageProps {
  titleKey: SectionTitleKey
  /** The content of the section. Without it the section shows that it is empty. */
  children?: ReactNode
}

/** A section of the app with its title. */
export default function SectionPage({ titleKey, children }: SectionPageProps) {
  const { t } = useTranslation()
  const title = t(titleKey)
  const appName = t('app.name')

  useEffect(() => {
    document.title = `${title} - ${appName}`
  }, [title, appName])

  return (
    <>
      <h1>{title}</h1>
      {children ?? <p className="placeholder">{t('sections.empty')}</p>}
    </>
  )
}
