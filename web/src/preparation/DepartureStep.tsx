import {
  useState,
  type FormEvent,
} from 'react'

import { ApiError } from '../api/api-client'
import {
  getTrackAnalysis,
  saveOutdoorContext,
  type OutdoorContext,
  type Track,
  type TrackAnalysis,
} from '../track/tracks-api'
import {
  browserTimeZone,
  defaultPlannedStart,
  normalizeLocalDateTime,
} from './preparation-flow'

export function DepartureStep({
  track,
  outdoorContext,
  onSaved,
  onUnauthorized,
  onBack,
}: {
  track: Track
  outdoorContext: OutdoorContext | null
  onSaved: (
    context: OutdoorContext,
    analysis: TrackAnalysis,
  ) => void
  onUnauthorized: () => void
  onBack: () => void
}) {
  const [plannedStartLocal, setPlannedStartLocal] = useState(
    outdoorContext?.plannedStartLocal.slice(0, 16) ?? defaultPlannedStart(),
  )
  const [plannedDurationMinutes, setPlannedDurationMinutes] = useState(
    outdoorContext?.plannedDurationMinutes ?? 360,
  )
  const [timeZone, setTimeZone] = useState(
    outdoorContext?.timeZone ?? browserTimeZone(),
  )
  const [weatherConsent, setWeatherConsent] = useState(
    outdoorContext !== null
      && outdoorContext.weather.status !== 'NOT_REQUESTED',
  )
  const [saving, setSaving] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage(null)
    setSaving(true)

    try {
      const savedContext = await saveOutdoorContext(track.id, {
        plannedStartLocal: normalizeLocalDateTime(plannedStartLocal),
        plannedDurationMinutes,
        timeZone: timeZone.trim(),
        shareStartPointWithWeatherProvider: weatherConsent,
      })

      try {
        const refreshedAnalysis = await getTrackAnalysis(track.id)
        onSaved(savedContext, refreshedAnalysis)
      } catch (analysisError: unknown) {
        if (analysisError instanceof ApiError && analysisError.status === 401) {
          onUnauthorized()
          return
        }

        setErrorMessage(
          'Le départ est enregistré, mais la préparation n’a pas pu être recalculée. Réessaie pour afficher des données à jour.',
        )
      }
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        onUnauthorized()
        return
      }

      setErrorMessage(messageForContextError(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section
      className="preparation-page departure-step"
      aria-labelledby="departure-title"
    >
      <button className="preparation-back" type="button" onClick={onBack}>
        <span aria-hidden="true">←</span>
        Vérifier la trace
      </button>

      <header className="preparation-heading">
        <p className="workspace-kicker">Étape 2 sur 3 · Le départ</p>
        <h1 id="departure-title">Quand comptes-tu partir&nbsp;?</h1>
        <p>
          L’horaire et la durée servent à calculer la lumière disponible. La
          météo reste facultative et demande ton accord séparément.
        </p>
      </header>

      <form className="departure-form" onSubmit={handleSubmit}>
        <div className="departure-fields">
          <label>
            <span>Date et heure de départ</span>
            <input
              type="datetime-local"
              value={plannedStartLocal}
              onChange={(event) => setPlannedStartLocal(event.target.value)}
              required
              disabled={saving}
            />
          </label>

          <label>
            <span>Durée prévue</span>
            <select
              value={plannedDurationMinutes}
              onChange={(event) =>
                setPlannedDurationMinutes(Number(event.target.value))
              }
              disabled={saving}
            >
              <option value={120}>2 heures</option>
              <option value={240}>4 heures</option>
              <option value={360}>6 heures</option>
              <option value={480}>8 heures</option>
              <option value={600}>10 heures</option>
              <option value={720}>12 heures</option>
            </select>
          </label>
        </div>

        <div className="departure-time-zone">
          <p>
            Les heures seront interprétées dans le fuseau{' '}
            <strong>{timeZone || 'non renseigné'}</strong>.
          </p>
          <details>
            <summary>Ce fuseau horaire est incorrect</summary>
            <label>
              <span>Fuseau du lieu de départ</span>
              <input
                type="text"
                value={timeZone}
                onChange={(event) => setTimeZone(event.target.value)}
                placeholder="Europe/Paris"
                maxLength={64}
                required
                autoComplete="off"
                disabled={saving}
              />
              <small>Nom IANA, par exemple Europe/Paris.</small>
            </label>
          </details>
        </div>

        <label className="weather-consent departure-weather-consent">
          <input
            type="checkbox"
            checked={weatherConsent}
            onChange={(event) => setWeatherConsent(event.target.checked)}
            disabled={saving}
          />
          <span>
            <strong>Ajouter la météo du point de départ</strong>
            <small>
              Facultatif. En cochant cette case, tu acceptes l’envoi ponctuel
              de cette seule coordonnée à Open‑Meteo. La trace complète n’est
              pas transmise.
            </small>
          </span>
        </label>

        {errorMessage && (
          <div className="form-alert" role="alert">
            <span aria-hidden="true">!</span>
            <p>{errorMessage}</p>
          </div>
        )}

        <div className="preparation-actions departure-actions">
          <button
            className="workspace-primary-action"
            type="submit"
            disabled={saving}
          >
            {saving && <span className="button-spinner" aria-hidden="true" />}
            {saving ? 'Calcul de la préparation…' : 'Voir ma préparation'}
            {!saving && <span aria-hidden="true">→</span>}
          </button>
          <button
            className="preparation-secondary-action"
            type="button"
            onClick={onBack}
            disabled={saving}
          >
            Retour à la trace
          </button>
        </div>
      </form>
    </section>
  )
}

function messageForContextError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return 'Le départ n’a pas pu être enregistré. Vérifie ta connexion puis réessaie.'
}
