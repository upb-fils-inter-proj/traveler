import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router'
import ApiErrorMessage from '../api/ApiErrorMessage.tsx'
import { listAttractions } from '../api/attractions.ts'
import { useApiClient, useResource } from '../api/hooks.ts'
import { attractionPath, NEW_ATTRACTION_PATH } from '../sections.ts'
import StatusLabel from './StatusLabel.tsx'

const INCLUDE_REMOVED = 'includeRemoved'

export default function AttractionListPage() {
  const { t } = useTranslation()
  const api = useApiClient()
  const headingId = useId()
  const [searchParams, setSearchParams] = useSearchParams()
  const includeRemoved = searchParams.get(INCLUDE_REMOVED) === 'true'
  const [attractions, , reload] = useResource(`attractions:${includeRemoved}`, (signal) =>
    listAttractions(api, includeRemoved, signal),
  )

  return (
    <section aria-labelledby={headingId}>
      <div className="toolbar">
        <h2 id={headingId}>{t('attractions.title')}</h2>
        <Link className="button" to={NEW_ATTRACTION_PATH}>
          {t('attractions.add')}
        </Link>
      </div>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={includeRemoved}
          onChange={(event) =>
            setSearchParams(event.target.checked ? { [INCLUDE_REMOVED]: 'true' } : {}, {
              replace: true,
            })
          }
        />
        {t('attractions.showRemoved')}
      </label>

      {attractions.status === 'loading' && <p role="status">{t('attractions.loading')}</p>}
      {attractions.status === 'error' && (
        <ApiErrorMessage error={attractions.error} onRetry={reload} />
      )}
      {attractions.status === 'ok' && attractions.data.length === 0 && (
        <p className="placeholder">{t('attractions.empty')}</p>
      )}
      {attractions.status === 'ok' && attractions.data.length > 0 && (
        <div className="table-wrapper">
          <table aria-labelledby={headingId}>
            <thead>
              <tr>
                <th scope="col">{t('attractions.columns.nameRo')}</th>
                <th scope="col">{t('attractions.columns.nameEn')}</th>
                <th scope="col">{t('attractions.columns.status')}</th>
                <th scope="col" className="number">
                  {t('attractions.columns.points')}
                </th>
                <th scope="col" className="number">
                  {t('attractions.columns.version')}
                </th>
              </tr>
            </thead>
            <tbody>
              {attractions.data.map((attraction) => (
                <tr key={attraction.id} className={`status-${attraction.status}`}>
                  <td>
                    <Link to={attractionPath(attraction.id)}>
                      {attraction.translations.ro.name}
                    </Link>
                  </td>
                  <td>{attraction.translations.en.name}</td>
                  <td>
                    <StatusLabel status={attraction.status} />
                  </td>
                  <td className="number">{attraction.pointValue}</td>
                  <td className="number">{attraction.version}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
