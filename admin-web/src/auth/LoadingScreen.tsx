import { useTranslation } from 'react-i18next'

export default function LoadingScreen({ message }: { message?: string }) {
  const { t } = useTranslation()

  return (
    <main className="screen">
      <p role="status">{message ?? t('common.loading')}</p>
    </main>
  )
}
