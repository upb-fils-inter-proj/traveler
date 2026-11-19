import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { createAttraction } from '../api/attractions.ts'
import { useApiClient } from '../api/hooks.ts'
import { ATTRACTION_LIST_PATH, attractionPath } from '../sections.ts'
import AttractionForm from './AttractionForm.tsx'

export default function NewAttractionPage() {
  const { t } = useTranslation()
  const api = useApiClient()
  const navigate = useNavigate()

  return (
    <section>
      <p>
        <Link to={ATTRACTION_LIST_PATH}>{t('attractions.backToList')}</Link>
      </p>
      <h2>{t('attractions.newTitle')}</h2>
      <AttractionForm
        submitLabel={t('attractions.form.create')}
        onSave={async (input) => {
          const created = await createAttraction(api, input)
          // The photo and the QR code need a saved attraction, so continue there.
          await navigate(attractionPath(created.id), { state: { created: true } })
        }}
      />
    </section>
  )
}
