import { useEffect, useId, useRef } from 'react'
import { useTranslation } from 'react-i18next'

interface ConfirmPanelProps {
  title: string
  text: string
  confirmLabel: string
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/** Asks before an action that cannot be undone. Cancel has the focus. */
export default function ConfirmPanel({
  title,
  text,
  confirmLabel,
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmPanelProps) {
  const { t } = useTranslation()
  const titleId = useId()
  const textId = useId()
  const cancelRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    cancelRef.current?.focus()
  }, [])

  return (
    <div
      className="confirm-panel"
      role="alertdialog"
      aria-labelledby={titleId}
      aria-describedby={textId}
      onKeyDown={(event) => {
        if (event.key === 'Escape') onCancel()
      }}
    >
      <p id={titleId} className="notice-title">
        {title}
      </p>
      <p id={textId}>{text}</p>
      <div className="button-row">
        <button type="button" className="danger" disabled={busy} onClick={onConfirm}>
          {confirmLabel}
        </button>
        <button type="button" className="secondary" ref={cancelRef} onClick={onCancel}>
          {t('attractions.actions.cancel')}
        </button>
      </div>
    </div>
  )
}
