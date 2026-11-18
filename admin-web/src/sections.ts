/** The administration sections, in the order of the side menu. */
export const SECTIONS = [
  { path: '/city-administrator', titleKey: 'sections.cityAdministrator' },
  { path: '/partner', titleKey: 'sections.partner' },
  { path: '/traveler-administration', titleKey: 'sections.travelerAdministration' },
] as const

export const DEFAULT_SECTION_PATH = SECTIONS[0].path
