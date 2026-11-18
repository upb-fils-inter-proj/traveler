import { useTranslation } from 'react-i18next'
import type { ConfigProblem } from '../config.ts'
import LanguageSwitcher from '../i18n/LanguageSwitcher.tsx'

export default function ConfigErrorPage({ problems }: { problems: ConfigProblem[] }) {
  const { t } = useTranslation()

  return (
    <main className="screen">
      <LanguageSwitcher />
      <h1>{t('configError.title')}</h1>
      <p role="alert" className="error">
        {t('configError.intro')}
      </p>
      <ul>
        {problems.map((problem) => (
          <li key={problem.variable}>
            <code>{problem.variable}</code> ({t(`configError.${problem.reason}`)})
          </li>
        ))}
      </ul>
      <p>{t('configError.hint')}</p>
    </main>
  )
}
