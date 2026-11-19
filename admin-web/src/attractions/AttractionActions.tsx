import { type ChangeEvent, useEffect, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import ApiErrorMessage from '../api/ApiErrorMessage.tsx'
import {
  type Attraction,
  type AttractionStatus,
  downloadQrCodePdf,
  fetchAttractionPhoto,
  regenerateQrCode,
  setAttractionStatus,
  uploadAttractionPhoto,
} from '../api/attractions.ts'
import { ApiError, toApiError } from '../api/client.ts'
import { useApiClient, useResource } from '../api/hooks.ts'
import ConfirmPanel from './ConfirmPanel.tsx'
import { convertToWebp, MAX_PHOTO_BYTES, PhotoConversionError, saveFile } from './files.ts'
import { useStatusText } from './statusText.ts'

interface ActionProps {
  attraction: Attraction
  /** Receives the attraction as the server returns it after a change. */
  onChange: (attraction: Attraction) => void
}

/** Runs one request at a time and keeps its error. */
function useAction() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  const run = async (action: () => Promise<void>): Promise<boolean> => {
    setBusy(true)
    setError(null)
    try {
      await action()
      return true
    } catch (caught) {
      setError(toApiError(caught))
      return false
    } finally {
      setBusy(false)
    }
  }

  return { busy, error, run }
}

function StatusActions({ attraction, onChange }: ActionProps) {
  const { t } = useTranslation()
  const api = useApiClient()
  const statusText = useStatusText()
  const { busy, error, run } = useAction()
  const [confirmRemove, setConfirmRemove] = useState(false)
  const [changedTo, setChangedTo] = useState<AttractionStatus | null>(null)

  const change = async (status: AttractionStatus) => {
    const done = await run(async () => {
      onChange(await setAttractionStatus(api, attraction.id, status))
    })
    if (done) {
      setChangedTo(status)
      setConfirmRemove(false)
    }
  }

  return (
    <section className="action-group">
      <h3>{t('attractions.actions.statusTitle')}</h3>
      <p>{t('attractions.actions.currentStatus', { status: statusText(attraction.status) })}</p>
      {changedTo && (
        <p role="status">
          {t('attractions.actions.statusChanged', { status: statusText(changedTo) })}
        </p>
      )}
      {error && <ApiErrorMessage error={error} />}
      {attraction.status !== 'removed' && !confirmRemove && (
        <div className="button-row">
          {attraction.status === 'open' ? (
            <button type="button" disabled={busy} onClick={() => void change('closed')}>
              {t('attractions.actions.close')}
            </button>
          ) : (
            <button type="button" disabled={busy} onClick={() => void change('open')}>
              {t('attractions.actions.reopen')}
            </button>
          )}
          <button
            type="button"
            className="danger"
            disabled={busy}
            onClick={() => setConfirmRemove(true)}
          >
            {t('attractions.actions.remove')}
          </button>
        </div>
      )}
      {confirmRemove && (
        <ConfirmPanel
          title={t('attractions.actions.removeConfirmTitle')}
          text={t('attractions.actions.removeConfirmText')}
          confirmLabel={t('attractions.actions.removeConfirm')}
          busy={busy}
          onConfirm={() => void change('removed')}
          onCancel={() => setConfirmRemove(false)}
        />
      )}
    </section>
  )
}

function PhotoActions({ attraction, onChange }: ActionProps) {
  const { t, i18n } = useTranslation()
  const nameLanguage = i18n.resolvedLanguage === 'ro' ? 'ro' : 'en'
  const api = useApiClient()
  const inputId = useId()
  const hintId = useId()
  const { busy, error, run } = useAction()
  const [problem, setProblem] = useState<
    'notImage' | 'unreadable' | 'webpUnsupported' | 'tooLarge' | null
  >(null)
  const [uploaded, setUploaded] = useState(false)

  const { id, hasPhoto, version } = attraction
  const [photo] = useResource(`photo:${id}:${hasPhoto}:${version}`, async (signal) => {
    if (!hasPhoto) return null
    const blob = await fetchAttractionPhoto(api, id, signal)
    return blob === null ? null : URL.createObjectURL(blob)
  })
  const photoUrl = photo.status === 'ok' ? photo.data : null
  useEffect(() => {
    return () => {
      if (photoUrl !== null) URL.revokeObjectURL(photoUrl)
    }
  }, [photoUrl])

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const input = event.target
    const file = input.files?.[0]
    input.value = ''
    if (file === undefined) return
    setProblem(null)
    setUploaded(false)
    if (!file.type.startsWith('image/')) {
      setProblem('notImage')
      return
    }

    let webp: Blob
    try {
      webp = await convertToWebp(file)
    } catch (caught) {
      setProblem(caught instanceof PhotoConversionError ? caught.reason : 'unreadable')
      return
    }
    if (webp.size > MAX_PHOTO_BYTES) {
      setProblem('tooLarge')
      return
    }
    const done = await run(async () => {
      onChange(await uploadAttractionPhoto(api, id, webp))
    })
    setUploaded(done)
  }

  const removed = attraction.status === 'removed'

  return (
    <section className="action-group">
      <h3>{t('attractions.photo.title')}</h3>
      {photo.status === 'loading' && <p role="status">{t('attractions.photo.loading')}</p>}
      {photo.status === 'error' && <p className="error">{t('attractions.photo.loadError')}</p>}
      {photo.status === 'ok' && photoUrl === null && (
        <p className="placeholder">{t('attractions.photo.none')}</p>
      )}
      {photoUrl !== null && (
        <img
          className="attraction-photo"
          src={photoUrl}
          alt={t('attractions.photo.alt', { name: attraction.translations[nameLanguage].name })}
        />
      )}
      {!removed && (
        <div className="field">
          <label htmlFor={inputId}>{t('attractions.photo.choose')}</label>
          <p id={hintId} className="hint">
            {t('attractions.photo.hint')}
          </p>
          <input
            id={inputId}
            type="file"
            accept="image/*"
            aria-describedby={hintId}
            disabled={busy}
            onChange={(event) => void upload(event)}
          />
        </div>
      )}
      {busy && <p role="status">{t('attractions.photo.uploading')}</p>}
      {uploaded && <p role="status">{t('attractions.photo.uploaded')}</p>}
      {problem && (
        <p className="error" role="alert">
          {t(`attractions.photo.problems.${problem}`)}
        </p>
      )}
      {error && <ApiErrorMessage error={error} />}
    </section>
  )
}

function QrActions({ attraction, onChange }: ActionProps) {
  const { t } = useTranslation()
  const api = useApiClient()
  const { busy, error, run } = useAction()
  const [confirmRegenerate, setConfirmRegenerate] = useState(false)
  const [regenerated, setRegenerated] = useState(false)

  const print = () =>
    run(async () => {
      const { pdf, fileName } = await downloadQrCodePdf(api, attraction)
      saveFile(pdf, fileName)
    })

  const regenerate = async () => {
    const done = await run(async () => {
      onChange(await regenerateQrCode(api, attraction.id))
    })
    if (done) {
      setRegenerated(true)
      setConfirmRegenerate(false)
    }
  }

  return (
    <section className="action-group">
      <h3>{t('attractions.qr.title')}</h3>
      <p>{t('attractions.qr.version', { version: attraction.qrVersion })}</p>
      {regenerated && <p role="status">{t('attractions.qr.regenerated')}</p>}
      {error && <ApiErrorMessage error={error} />}
      {!confirmRegenerate && (
        <div className="button-row">
          <button type="button" disabled={busy} onClick={() => void print()}>
            {t('attractions.qr.print')}
          </button>
          <button
            type="button"
            className="danger"
            disabled={busy}
            onClick={() => setConfirmRegenerate(true)}
          >
            {t('attractions.qr.regenerate')}
          </button>
        </div>
      )}
      {confirmRegenerate && (
        <ConfirmPanel
          title={t('attractions.qr.regenerateConfirmTitle')}
          text={t('attractions.qr.regenerateConfirmText')}
          confirmLabel={t('attractions.qr.regenerateConfirm')}
          busy={busy}
          onConfirm={() => void regenerate()}
          onCancel={() => setConfirmRegenerate(false)}
        />
      )}
    </section>
  )
}

/** Status, photo and QR code of a saved attraction. */
export default function AttractionActions(props: ActionProps) {
  const removed = props.attraction.status === 'removed'
  return (
    <div className="attraction-actions">
      <StatusActions {...props} />
      <PhotoActions {...props} />
      {!removed && <QrActions {...props} />}
    </div>
  )
}
