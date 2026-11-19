import { type FormEvent, lazy, Suspense, useCallback, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import ApiErrorMessage from '../api/ApiErrorMessage.tsx'
import type { Attraction, AttractionInput, LatLon } from '../api/attractions.ts'
import { ApiError, type FieldError, toApiError } from '../api/client.ts'
import {
  CONTENT_LANGUAGES,
  type ContentLanguage,
  emptyValues,
  type FieldPath,
  type FormValues,
  formatCoordinate,
  isFieldPath,
  type Issue,
  normalizeFieldPath,
  positionOf,
  toInput,
  type TranslationValues,
  validate,
  valuesFrom,
} from './formValues.ts'

// Leaflet is only downloaded when a form is opened.
const LocationMap = lazy(() => import('./LocationMap.tsx'))

const LANGUAGE_LEGEND_KEYS = {
  ro: 'attractions.form.romanian',
  en: 'attractions.form.english',
} as const

interface TextFieldProps {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  hint?: string
  multiline?: boolean
  inputMode?: 'decimal' | 'numeric'
}

function TextField({ id, label, value, onChange, error, hint, multiline, inputMode }: TextFieldProps) {
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined
  const common = {
    id,
    value,
    'aria-invalid': error ? true : undefined,
    'aria-describedby': describedBy,
  }

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {hint && (
        <p id={hintId} className="hint">
          {hint}
        </p>
      )}
      {multiline ? (
        <textarea {...common} rows={5} onChange={(event) => onChange(event.target.value)} />
      ) : (
        <input
          {...common}
          type="text"
          inputMode={inputMode}
          onChange={(event) => onChange(event.target.value)}
        />
      )}
      {error && (
        <p id={errorId} className="field-error">
          {error}
        </p>
      )}
    </div>
  )
}

interface AttractionFormProps {
  /** The attraction to edit, or nothing to create one. */
  attraction?: Attraction
  submitLabel: string
  /** No changes possible, for removed attractions. */
  readOnly?: boolean
  /** Saves the input; rejects with an ApiError the form shows. */
  onSave: (input: AttractionInput) => Promise<void>
}

/** The fields of an attraction in both languages, its points and its map pin. */
export default function AttractionForm({
  attraction,
  submitLabel,
  readOnly = false,
  onSave,
}: AttractionFormProps) {
  const { t } = useTranslation()
  const idPrefix = useId()
  const mode = attraction ? 'edit' : 'create'
  const [values, setValues] = useState<FormValues>(() =>
    attraction ? valuesFrom(attraction) : emptyValues(),
  )
  const [submitted, setSubmitted] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<ApiError | null>(null)
  const [serverErrors, setServerErrors] = useState<Partial<Record<FieldPath, string>>>({})

  const issues = submitted ? validate(values, mode) : {}
  const issueText = (issue: Issue) =>
    issue.code === 'tooLong'
      ? t('attractions.form.errors.tooLong', { max: issue.max })
      : t(`attractions.form.errors.${issue.code}`)
  const errorFor = (path: FieldPath): string | undefined => {
    const issue = issues[path]
    return issue ? issueText(issue) : serverErrors[path]
  }
  const fieldId = (path: FieldPath) => `${idPrefix}-${path.replaceAll('.', '-')}`

  const clearServerErrors = (...paths: FieldPath[]) => {
    setServerErrors((current) => {
      if (!paths.some((path) => path in current)) return current
      const next = { ...current }
      for (const path of paths) delete next[path]
      return next
    })
  }

  const setField = (path: 'pointValue' | 'visitMinutes' | 'lat' | 'lon', value: string) => {
    setValues((current) => ({ ...current, [path]: value }))
    clearServerErrors(
      path === 'lat' ? 'location.lat' : path === 'lon' ? 'location.lon' : path,
      'location',
    )
  }

  const setTranslation = (
    language: ContentLanguage,
    field: keyof TranslationValues,
    value: string,
  ) => {
    setValues((current) => ({
      ...current,
      translations: {
        ...current.translations,
        [language]: { ...current.translations[language], [field]: value },
      },
    }))
    clearServerErrors(`translations.${language}.${field}`)
  }

  const placePin = useCallback((position: LatLon) => {
    setValues((current) => ({
      ...current,
      lat: formatCoordinate(position.lat),
      lon: formatCoordinate(position.lon),
    }))
    setServerErrors((current) => {
      const next = { ...current }
      delete next.location
      delete next['location.lat']
      delete next['location.lon']
      return next
    })
  }, [])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitted(true)
    if (Object.keys(validate(values, mode)).length > 0) return

    setSaving(true)
    setSaveError(null)
    setServerErrors({})
    try {
      await onSave(toInput(values))
    } catch (error) {
      const apiError = toApiError(error)
      const byField: Partial<Record<FieldPath, string>> = {}
      for (const fieldError of apiError.fieldErrors) {
        const path = normalizeFieldPath(fieldError.field)
        if (isFieldPath(path) && byField[path] === undefined) byField[path] = fieldError.message
      }
      setServerErrors(byField)
      setSaveError(apiError)
    } finally {
      setSaving(false)
    }
  }

  const unmatchedErrors: FieldError[] =
    saveError?.fieldErrors.filter((item) => !isFieldPath(normalizeFieldPath(item.field))) ?? []
  const hasIssues = Object.keys(issues).length > 0

  return (
    <form className="attraction-form" noValidate onSubmit={(event) => void handleSubmit(event)}>
      {saveError && <ApiErrorMessage error={saveError} fieldErrors={unmatchedErrors} />}
      {hasIssues && (
        <p className="error" role="alert">
          {t('attractions.form.fixErrors')}
        </p>
      )}

      <fieldset className="plain" disabled={readOnly || saving}>
        <div className="translation-columns">
          {CONTENT_LANGUAGES.map((language) => {
            const field = (name: keyof TranslationValues) => `translations.${language}.${name}` as const
            return (
              <fieldset key={language}>
                <legend>{t(LANGUAGE_LEGEND_KEYS[language])}</legend>
                <TextField
                  id={fieldId(field('name'))}
                  label={t('attractions.form.name')}
                  value={values.translations[language].name}
                  onChange={(value) => setTranslation(language, 'name', value)}
                  error={errorFor(field('name'))}
                />
                <TextField
                  id={fieldId(field('description'))}
                  label={t('attractions.form.description')}
                  value={values.translations[language].description}
                  onChange={(value) => setTranslation(language, 'description', value)}
                  error={errorFor(field('description'))}
                  multiline
                />
                <TextField
                  id={fieldId(field('history'))}
                  label={t('attractions.form.history')}
                  value={values.translations[language].history}
                  onChange={(value) => setTranslation(language, 'history', value)}
                  error={errorFor(field('history'))}
                  multiline
                />
                <TextField
                  id={fieldId(field('sources'))}
                  label={t('attractions.form.sources')}
                  hint={t('attractions.form.sourcesHint')}
                  value={values.translations[language].sources}
                  onChange={(value) => setTranslation(language, 'sources', value)}
                  error={errorFor(field('sources'))}
                  multiline
                />
              </fieldset>
            )
          })}
        </div>

        <fieldset>
          <legend>{t('attractions.form.details')}</legend>
          <div className="field-row">
            <TextField
              id={fieldId('pointValue')}
              label={t('attractions.form.pointValue')}
              value={values.pointValue}
              onChange={(value) => setField('pointValue', value)}
              error={errorFor('pointValue')}
              inputMode="numeric"
            />
            <TextField
              id={fieldId('visitMinutes')}
              label={t('attractions.form.visitMinutes')}
              value={values.visitMinutes}
              onChange={(value) => setField('visitMinutes', value)}
              error={errorFor('visitMinutes')}
              inputMode="numeric"
            />
          </div>
        </fieldset>

        <fieldset>
          <legend>{t('attractions.form.location')}</legend>
          <p className="hint">{t('attractions.form.mapHint')}</p>
          <Suspense fallback={<p role="status">{t('attractions.form.mapLoading')}</p>}>
            <LocationMap position={positionOf(values)} onChange={placePin} disabled={readOnly} />
          </Suspense>
          <div className="field-row">
            <TextField
              id={fieldId('location.lat')}
              label={t('attractions.form.latitude')}
              value={values.lat}
              onChange={(value) => setField('lat', value)}
              error={errorFor('location.lat')}
              inputMode="decimal"
            />
            <TextField
              id={fieldId('location.lon')}
              label={t('attractions.form.longitude')}
              value={values.lon}
              onChange={(value) => setField('lon', value)}
              error={errorFor('location.lon')}
              inputMode="decimal"
            />
          </div>
          {errorFor('location') && <p className="field-error">{errorFor('location')}</p>}
        </fieldset>

        {!readOnly && (
          <div className="button-row">
            <button type="submit">{saving ? t('attractions.form.saving') : submitLabel}</button>
          </div>
        )}
      </fieldset>
    </form>
  )
}
