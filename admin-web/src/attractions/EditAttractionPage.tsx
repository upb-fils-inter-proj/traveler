import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useParams } from 'react-router'
import ApiErrorMessage from '../api/ApiErrorMessage.tsx'
import { getAttraction, updateAttraction } from '../api/attractions.ts'
import { useApiClient, useResource } from '../api/hooks.ts'
import { ATTRACTION_LIST_PATH } from '../sections.ts'
import AttractionActions from './AttractionActions.tsx'
import AttractionForm from './AttractionForm.tsx'

export default function EditAttractionPage() {
  const { t } = useTranslation()
  const api = useApiClient()
  const { id = '' } = useParams()
  const location = useLocation()
  const justCreated = (location.state as { created?: boolean } | null)?.created === true
  const [attraction, setAttraction, reload] = useResource(`attraction:${id}`, (signal) =>
    getAttraction(api, id, signal),
  )
  const [saved, setSaved] = useState(justCreated)

  return (
    <section>
      <p>
        <Link to={ATTRACTION_LIST_PATH}>{t('attractions.backToList')}</Link>
      </p>
      {attraction.status === 'loading' && <p role="status">{t('attractions.loadingOne')}</p>}
      {attraction.status === 'error' && (
        <ApiErrorMessage error={attraction.error} onRetry={reload} />
      )}
      {attraction.status === 'ok' && (
        <>
          <h2>
            {t('attractions.editTitle', { name: attraction.data.translations.ro.name })}
          </h2>
          {saved && <p role="status">{t('attractions.saved')}</p>}
          {attraction.data.status === 'removed' && (
            <p className="notice">{t('attractions.removedNotice')}</p>
          )}
          <div className="attraction-page">
            <AttractionForm
              key={attraction.data.id}
              attraction={attraction.data}
              submitLabel={t('attractions.form.save')}
              readOnly={attraction.data.status === 'removed'}
              onSave={async (input) => {
                setSaved(false)
                setAttraction(await updateAttraction(api, id, input))
                setSaved(true)
              }}
            />
            <AttractionActions
              attraction={attraction.data}
              onChange={(updated) => {
                setSaved(false)
                setAttraction(updated)
              }}
            />
          </div>
        </>
      )}
    </section>
  )
}
