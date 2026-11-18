import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router'
import { SECTIONS } from '../sections.ts'

export default function SideMenu() {
  const { t } = useTranslation()

  return (
    <nav className="side-menu" aria-label={t('menu.label')}>
      <ul>
        {SECTIONS.map((section) => (
          <li key={section.path}>
            <NavLink to={section.path}>{t(section.titleKey)}</NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
