import type { AttractionStatus } from '../api/attractions.ts'
import { useStatusText } from './statusText.ts'

export default function StatusLabel({ status }: { status: AttractionStatus }) {
  const statusText = useStatusText()
  return <span className={`status status-${status}`}>{statusText(status)}</span>
}
