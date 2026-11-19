import { ApiError, type ApiClient, jsonBody } from './client.ts'

export type AttractionStatus = 'open' | 'closed' | 'removed'

export interface LatLon {
  lat: number
  lon: number
}

export interface Translation {
  name: string
  description: string
  history: string | null
  sources: string | null
}

export interface Translations {
  ro: Translation
  en: Translation
}

export interface Attraction {
  id: string
  status: AttractionStatus
  location: LatLon
  pointValue: number
  visitMinutes: number
  dangerRadiusM: number
  dangerFlag: boolean
  dangerMessage: string | null
  qrVersion: number
  hasPhoto: boolean
  version: number
  translations: Translations
}

/** Body of create and update. visitMinutes may be left out on create. */
export interface AttractionInput {
  location: LatLon
  pointValue: number
  visitMinutes?: number
  translations: Translations
}

export interface Me {
  email: string
  role: string
}

/** Roles that may manage attractions. */
const ATTRACTION_ROLES: readonly string[] = ['city_admin', 'traveler_admin']

export function canManageAttractions(role: string): boolean {
  return ATTRACTION_ROLES.includes(role)
}

const ATTRACTIONS = '/admin/attractions'

function attractionUrl(id: string): string {
  return `${ATTRACTIONS}/${encodeURIComponent(id)}`
}

export function fetchMe(api: ApiClient, signal?: AbortSignal): Promise<Me> {
  return api.json<Me>('/admin/me', { signal })
}

export async function listAttractions(
  api: ApiClient,
  includeRemoved: boolean,
  signal?: AbortSignal,
): Promise<Attraction[]> {
  const body = await api.json<{ items: Attraction[] }>(
    `${ATTRACTIONS}?includeRemoved=${includeRemoved}`,
    { signal },
  )
  return body.items
}

export function getAttraction(api: ApiClient, id: string, signal?: AbortSignal) {
  return api.json<Attraction>(attractionUrl(id), { signal })
}

export function createAttraction(api: ApiClient, input: AttractionInput) {
  return api.json<Attraction>(ATTRACTIONS, jsonBody('POST', input))
}

export function updateAttraction(api: ApiClient, id: string, input: AttractionInput) {
  return api.json<Attraction>(attractionUrl(id), jsonBody('PUT', input))
}

export function setAttractionStatus(api: ApiClient, id: string, status: AttractionStatus) {
  return api.json<Attraction>(`${attractionUrl(id)}/status`, jsonBody('PUT', { status }))
}

/** Uploads the photo as raw WebP bytes. */
export function uploadAttractionPhoto(api: ApiClient, id: string, photo: Blob) {
  return api.json<Attraction>(`${attractionUrl(id)}/photo`, {
    method: 'PUT',
    headers: { 'Content-Type': 'image/webp' },
    body: photo,
  })
}

/** The current photo, or null when the attraction has none. */
export async function fetchAttractionPhoto(
  api: ApiClient,
  id: string,
  signal?: AbortSignal,
): Promise<Blob | null> {
  try {
    const response = await api.send(`${attractionUrl(id)}/photo`, { signal })
    return await response.blob()
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

export function regenerateQrCode(api: ApiClient, id: string) {
  return api.json<Attraction>(`${attractionUrl(id)}/qr-code/regenerate`, { method: 'POST' })
}

/** Reads the file name from a Content-Disposition header. */
export function fileNameFrom(contentDisposition: string | null): string | null {
  const match = /filename="?([^";]+)"?/i.exec(contentDisposition ?? '')
  return match?.[1]?.trim() || null
}

export async function downloadQrCodePdf(
  api: ApiClient,
  attraction: Pick<Attraction, 'id' | 'qrVersion'>,
): Promise<{ pdf: Blob; fileName: string }> {
  const response = await api.send(`${attractionUrl(attraction.id)}/qr-code.pdf`)
  const fileName =
    fileNameFrom(response.headers.get('Content-Disposition')) ??
    `qr-${attraction.id}-v${attraction.qrVersion}.pdf`
  return { pdf: await response.blob(), fileName }
}
