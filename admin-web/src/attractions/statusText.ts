import { useTranslation } from 'react-i18next'
import type { AttractionStatus } from '../api/attractions.ts'

const STATUS_KEYS = {
  open: 'attractions.status.open',
  closed: 'attractions.status.closed',
  removed: 'attractions.status.removed',
} as const

/** Translates an attraction status. */
export function useStatusText() {
  const { t } = useTranslation()
  return (status: AttractionStatus) => t(STATUS_KEYS[status])
}
