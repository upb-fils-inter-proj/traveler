import { useTranslation } from 'react-i18next'
import type { ApiError, FieldError } from './client.ts'

function summaryKey(status: number) {
  switch (status) {
    case 0:
      return 'api.errors.network'
    case 400:
      return 'api.errors.invalid'
    case 401:
      return 'api.errors.unauthorized'
    case 403:
      return 'api.errors.forbidden'
    case 404:
      return 'api.errors.notFound'
    case 409:
      return 'api.errors.conflict'
    case 413:
      return 'api.errors.tooLarge'
    case 415:
      return 'api.errors.unsupportedType'
    default:
      return 'api.errors.generic'
  }
}

interface ApiErrorMessageProps {
  error: ApiError
  /** Field errors to list here. Defaults to all of them. */
  fieldErrors?: readonly FieldError[]
  onRetry?: () => void
}

/** A translated summary of a failed request, with the server's detail. */
export default function ApiErrorMessage({ error, fieldErrors, onRetry }: ApiErrorMessageProps) {
  const { t } = useTranslation()
  const listed = fieldErrors ?? error.fieldErrors

  return (
    <div className="error-box" role="alert">
      <p>{t(summaryKey(error.status), { status: error.status })}</p>
      {error.detail !== null && <p>{t('api.detail', { detail: error.detail })}</p>}
      {listed.length > 0 && (
        <ul>
          {listed.map((item) => (
            <li key={`${item.field}:${item.message}`}>
              <code>{item.field}</code>: {item.message}
            </li>
          ))}
        </ul>
      )}
      {onRetry && (
        <button type="button" className="secondary" onClick={onRetry}>
          {t('api.retry')}
        </button>
      )}
    </div>
  )
}
