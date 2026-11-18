import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import SideMenu from './SideMenu.tsx'

describe('SideMenu', () => {
  it('lists the three sections', () => {
    render(
      <MemoryRouter>
        <SideMenu />
      </MemoryRouter>,
    )

    const links = within(
      screen.getByRole('navigation', { name: 'Main navigation' }),
    ).getAllByRole('link')

    expect(links.map((link) => link.textContent)).toEqual([
      'City Administrator',
      'Partner',
      'Traveler Administration',
    ])
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/city-administrator',
      '/partner',
      '/traveler-administration',
    ])
  })

  it('marks the current section', () => {
    render(
      <MemoryRouter initialEntries={['/partner']}>
        <SideMenu />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Partner' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(
      screen.getByRole('link', { name: 'City Administrator' }),
    ).not.toHaveAttribute('aria-current')
  })
})
