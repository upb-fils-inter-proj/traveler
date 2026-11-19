import type { Attraction, AttractionInput, LatLon, Translation } from '../api/attractions.ts'

/** Limits of the admin API contract. */
export const LIMITS = { name: 120, description: 4000, longText: 20000 } as const
export const DEFAULT_VISIT_MINUTES = 15

export const CONTENT_LANGUAGES = ['ro', 'en'] as const
export type ContentLanguage = (typeof CONTENT_LANGUAGES)[number]

export interface TranslationValues {
  name: string
  description: string
  history: string
  sources: string
}

/** What the form holds: text as typed. */
export interface FormValues {
  lat: string
  lon: string
  pointValue: string
  visitMinutes: string
  translations: Record<ContentLanguage, TranslationValues>
}

/** Field names as the API reports them in validation errors. */
export type FieldPath =
  | 'location'
  | 'location.lat'
  | 'location.lon'
  | 'pointValue'
  | 'visitMinutes'
  | `translations.${ContentLanguage}.${keyof TranslationValues}`

export type Issue =
  | { code: 'required' | 'wholeNumber' | 'number' | 'latitudeRange' | 'longitudeRange' | 'locationRequired' }
  | { code: 'tooLong'; max: number }

export type Issues = Partial<Record<FieldPath, Issue>>

const TRANSLATION_FIELDS = ['name', 'description', 'history', 'sources'] as const

export const FIELD_PATHS: readonly FieldPath[] = [
  'location',
  'location.lat',
  'location.lon',
  'pointValue',
  'visitMinutes',
  ...CONTENT_LANGUAGES.flatMap((language) =>
    TRANSLATION_FIELDS.map((field) => `translations.${language}.${field}` as const),
  ),
]

export function isFieldPath(value: string): value is FieldPath {
  return FIELD_PATHS.some((path) => path === value)
}

/** Spring reports map entries as translations[ro].name: use dots like the contract. */
export function normalizeFieldPath(field: string): string {
  return field.replace(/\[(\w+)\]/g, '.$1')
}

const emptyTranslation: TranslationValues = { name: '', description: '', history: '', sources: '' }

export function emptyValues(): FormValues {
  return {
    lat: '',
    lon: '',
    pointValue: '',
    visitMinutes: String(DEFAULT_VISIT_MINUTES),
    translations: { ro: { ...emptyTranslation }, en: { ...emptyTranslation } },
  }
}

function translationValues(translation: Translation): TranslationValues {
  return {
    name: translation.name,
    description: translation.description,
    history: translation.history ?? '',
    sources: translation.sources ?? '',
  }
}

export function valuesFrom(attraction: Attraction): FormValues {
  return {
    lat: String(attraction.location.lat),
    lon: String(attraction.location.lon),
    pointValue: String(attraction.pointValue),
    visitMinutes: String(attraction.visitMinutes),
    translations: {
      ro: translationValues(attraction.translations.ro),
      en: translationValues(attraction.translations.en),
    },
  }
}

/** A decimal number, accepting a comma as the decimal separator. */
export function parseDecimal(value: string): number | null {
  const trimmed = value.trim()
  if (!/^[-+]?(\d+([.,]\d*)?|[.,]\d+)$/.test(trimmed)) return null
  return Number(trimmed.replace(',', '.'))
}

function parseWholeNumber(value: string): number | null {
  const trimmed = value.trim()
  if (!/^\d+$/.test(trimmed)) return null
  const number = Number(trimmed)
  return Number.isSafeInteger(number) && number > 0 ? number : null
}

/** The pin position when both coordinates are valid. */
export function positionOf(values: FormValues): LatLon | null {
  const lat = parseDecimal(values.lat)
  const lon = parseDecimal(values.lon)
  if (lat === null || lon === null) return null
  if (Math.abs(lat) > 90 || Math.abs(lon) > 180) return null
  return { lat, lon }
}

/** Formats a coordinate with 6 decimals (about 10 cm), without trailing zeros. */
export function formatCoordinate(value: number): string {
  return String(Number(value.toFixed(6)))
}

/** The same rules as the admin API, so most mistakes are caught before sending. */
export function validate(values: FormValues, mode: 'create' | 'edit'): Issues {
  const issues: Issues = {}

  const lat = values.lat.trim()
  const lon = values.lon.trim()
  if (lat === '' && lon === '') {
    issues.location = { code: 'locationRequired' }
  } else {
    const coordinate = (text: string, max: number, range: 'latitudeRange' | 'longitudeRange'): Issue | null => {
      if (text === '') return { code: 'required' }
      const number = parseDecimal(text)
      if (number === null) return { code: 'number' }
      return Math.abs(number) > max ? { code: range } : null
    }
    const latIssue = coordinate(lat, 90, 'latitudeRange')
    const lonIssue = coordinate(lon, 180, 'longitudeRange')
    if (latIssue) issues['location.lat'] = latIssue
    if (lonIssue) issues['location.lon'] = lonIssue
  }

  if (values.pointValue.trim() === '') {
    issues.pointValue = { code: 'required' }
  } else if (parseWholeNumber(values.pointValue) === null) {
    issues.pointValue = { code: 'wholeNumber' }
  }

  if (values.visitMinutes.trim() === '') {
    if (mode === 'edit') issues.visitMinutes = { code: 'required' }
  } else if (parseWholeNumber(values.visitMinutes) === null) {
    issues.visitMinutes = { code: 'wholeNumber' }
  }

  for (const language of CONTENT_LANGUAGES) {
    const translation = values.translations[language]
    const required = (field: 'name' | 'description', max: number) => {
      const text = translation[field].trim()
      if (text === '') {
        issues[`translations.${language}.${field}`] = { code: 'required' }
      } else if (text.length > max) {
        issues[`translations.${language}.${field}`] = { code: 'tooLong', max }
      }
    }
    required('name', LIMITS.name)
    required('description', LIMITS.description)
    for (const field of ['history', 'sources'] as const) {
      if (translation[field].length > LIMITS.longText) {
        issues[`translations.${language}.${field}`] = { code: 'tooLong', max: LIMITS.longText }
      }
    }
  }

  return issues
}

function translationInput(values: TranslationValues): Translation {
  const optional = (text: string) => (text.trim() === '' ? null : text)
  return {
    name: values.name.trim(),
    description: values.description.trim(),
    history: optional(values.history),
    sources: optional(values.sources),
  }
}

/** The request body for values that passed validation. */
export function toInput(values: FormValues): AttractionInput {
  const input: AttractionInput = {
    location: {
      lat: parseDecimal(values.lat) ?? Number.NaN,
      lon: parseDecimal(values.lon) ?? Number.NaN,
    },
    pointValue: Number(values.pointValue.trim()),
    translations: {
      ro: translationInput(values.translations.ro),
      en: translationInput(values.translations.en),
    },
  }
  if (values.visitMinutes.trim() !== '') {
    input.visitMinutes = Number(values.visitMinutes.trim())
  }
  return input
}
