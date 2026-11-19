/** Route of the City Administrator section, which holds the attractions. */
export const CITY_ADMINISTRATOR_PATH = '/city-administrator'

/** The administration sections, in the order of the side menu. */
export const SECTIONS = [
  { path: CITY_ADMINISTRATOR_PATH, titleKey: 'sections.cityAdministrator' },
  { path: '/partner', titleKey: 'sections.partner' },
  { path: '/traveler-administration', titleKey: 'sections.travelerAdministration' },
] as const

export const DEFAULT_SECTION_PATH = SECTIONS[0].path

/** Routes of the attraction screens, inside the City Administrator section. */
export const ATTRACTION_LIST_PATH = CITY_ADMINISTRATOR_PATH
export const NEW_ATTRACTION_PATH = `${CITY_ADMINISTRATOR_PATH}/attractions/new`

export function attractionPath(id: string): string {
  return `${CITY_ADMINISTRATOR_PATH}/attractions/${encodeURIComponent(id)}`
}
