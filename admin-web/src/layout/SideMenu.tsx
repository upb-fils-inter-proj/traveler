import { NavLink } from 'react-router'
import { SECTIONS } from '../sections.ts'

export default function SideMenu() {
  return (
    <nav className="side-menu" aria-label="Main navigation">
      <ul>
        {SECTIONS.map((section) => (
          <li key={section.path}>
            <NavLink to={section.path}>{section.title}</NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
