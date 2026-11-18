/** The administration sections, in the order of the side menu. */
export const SECTIONS = [
  { path: '/city-administrator', title: 'City Administrator' },
  { path: '/partner', title: 'Partner' },
  { path: '/traveler-administration', title: 'Traveler Administration' },
] as const

export const DEFAULT_SECTION_PATH = SECTIONS[0].path
