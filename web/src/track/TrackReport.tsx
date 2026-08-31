import {
  useState,
  type FormEvent,
} from 'react'

import { ApiError } from '../api/api-client'
import { TrackAnalysisPanel } from './TrackAnalysisPanel'
import { TrackFeedbackPanel } from './TrackFeedbackPanel'
import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatGrade,
  formatMeters,
} from './track-format'
import {
  getTrackAnalysis,
  saveOutdoorContext,
  type OutdoorContext,
  type Track,
  type TrackAnalysis,
  type WeatherContext,
} from './tracks-api'

export function TrackReport({
  track,
  outdoorContext,
  analysis,
  onOutdoorContextChange,
  onAnalysisChange,
  onUnauthorized,
  onBack,
}: {
  track: Track
  outdoorContext: OutdoorContext | null
  analysis: TrackAnalysis
  onOutdoorContextChange: (context: OutdoorContext) => void
  onAnalysisChange: (analysis: TrackAnalysis) => void
  onUnauthorized: () => void
  onBack: () => void
}) {
  const facts = track.facts
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
  const [savingContext, setSavingContext] = useState(false)
  const [contextError, setContextError] = useState<string | null>(null)
  const [contextSuccess, setContextSuccess] = useState<string | null>(null)

  async function handleContextSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setContextError(null)
    setContextSuccess(null)
    setSavingContext(true)

    try {
      const saved = await saveOutdoorContext(track.id, {
        plannedStartLocal: normalizeLocalDateTime(plannedStartLocal),
        plannedDurationMinutes,
        timeZone: timeZone.trim(),
        shareStartPointWithWeatherProvider: weatherConsent,
      })

      onOutdoorContextChange(saved)
      const savedMessage =
        weatherConsent
          ? 'Horaire enregistré et prévision météo actualisée.'
          : 'Horaire enregistré. Aucun point GPS n’a été transmis.'

      try {
        const refreshedAnalysis = await getTrackAnalysis(track.id)
        onAnalysisChange(refreshedAnalysis)
        setContextSuccess(savedMessage)
      } catch (analysisError: unknown) {
        if (analysisError instanceof ApiError && analysisError.status === 401) {
          onUnauthorized()
          return
        }

        setContextSuccess(
          `${savedMessage} Recharge le rapport pour actualiser les règles.`,
        )
      }
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        onUnauthorized()
        return
      }

      setContextError(messageForContextError(error))
    } finally {
      setSavingContext(false)
    }
  }

  return (
    <main className="track-report">
      <nav className="report-navigation" aria-label="Navigation du rapport">
        <button type="button" onClick={onBack}>
          <span aria-hidden="true">←</span>
          Mes traces
        </button>
        <span>Rapport explicable · version 3</span>
      </nav>

      <header className="report-heading">
        <div>
          <p className="eyebrow">
            <span aria-hidden="true">●</span>
            Analyse prudente
          </p>
          <h1>{track.name}</h1>
          <p>{track.sourceFilename} · {formatCoverage(track)}</p>
        </div>
        <time dateTime={track.createdAt}>
          Importée le {formatReportDate(track.createdAt)}
        </time>
      </header>

      <aside className="report-caution" aria-label="Limite du rapport">
        <span aria-hidden="true">i</span>
        <div>
          <strong>Des faits de préparation, jamais un feu vert.</strong>
          <p>
            La trace, la lumière et une prévision ponctuelle ne suffisent pas
            à déclarer une sortie sûre. Vérifie toujours les bulletins locaux,
            le terrain, les alertes et les capacités réelles du groupe.
          </p>
        </div>
      </aside>

      {facts ? (
        <section className="report-metrics" aria-label="Faits principaux">
          <ReportMetric
            label="Distance"
            value={formatDistance(facts.distanceMeters)}
            detail="Somme WGS84, segments séparés"
          />
          <ReportMetric
            label="Dénivelé positif"
            value={formatMeters(facts.elevationGainMeters)}
            detail={track.elevationComplete ? 'Altitude complète' : 'Valeur partielle'}
          />
          <ReportMetric
            label="Dénivelé négatif"
            value={formatMeters(facts.elevationLossMeters)}
            detail={track.elevationComplete ? 'Altitude complète' : 'Valeur partielle'}
          />
          <ReportMetric
            label="Plage d’altitude"
            value={formatElevationRange(facts)}
            detail="Minimum–maximum du GPX"
          />
          <ReportMetric
            label="Pente montante max"
            value={formatGrade(facts.maximumUphillGradePercent, '+')}
            detail={`Fenêtre ≥ ${facts.gradeMinimumRunMeters} m`}
          />
          <ReportMetric
            label="Pente descendante max"
            value={formatGrade(facts.maximumDownhillGradePercent, '−')}
            detail={`Fenêtre ≥ ${facts.gradeMinimumRunMeters} m`}
          />
        </section>
      ) : (
        <section className="report-unavailable" role="status">
          <strong>Calcul des faits en attente</strong>
          <p>Recharge la bibliothèque pour relancer l’analyse de cette trace.</p>
        </section>
      )}

      <TrackFeedbackPanel
        trackId={track.id}
        onUnauthorized={onUnauthorized}
      />

      <section className="outdoor-context" aria-labelledby="outdoor-title">
        <div className="outdoor-heading">
          <div>
            <p className="auth-kicker">Date, lumière et météo</p>
            <h2 id="outdoor-title">Planifier cette sortie</h2>
          </div>
          {outdoorContext && (
            <span>
              Contexte mis à jour le {formatCompactDate(outdoorContext.updatedAt)}
            </span>
          )}
        </div>

        <form className="outdoor-form" onSubmit={handleContextSave}>
          <div className="outdoor-form-grid">
            <label>
              <span>Départ local</span>
              <input
                type="datetime-local"
                value={plannedStartLocal}
                onChange={(event) => setPlannedStartLocal(event.target.value)}
                required
                disabled={savingContext}
              />
            </label>
            <label>
              <span>Durée prévue</span>
              <select
                value={plannedDurationMinutes}
                onChange={(event) =>
                  setPlannedDurationMinutes(Number(event.target.value))
                }
                disabled={savingContext}
              >
                <option value={120}>2 heures</option>
                <option value={240}>4 heures</option>
                <option value={360}>6 heures</option>
                <option value={480}>8 heures</option>
                <option value={600}>10 heures</option>
                <option value={720}>12 heures</option>
              </select>
            </label>
            <label>
              <span>Fuseau du départ</span>
              <input
                type="text"
                value={timeZone}
                onChange={(event) => setTimeZone(event.target.value)}
                placeholder="Europe/Paris"
                maxLength={64}
                required
                autoComplete="off"
                disabled={savingContext}
              />
              <small>Format IANA, par exemple Europe/Paris.</small>
            </label>
          </div>

          <label className="weather-consent">
            <input
              type="checkbox"
              checked={weatherConsent}
              onChange={(event) => setWeatherConsent(event.target.checked)}
              disabled={savingContext}
            />
            <span>
              <strong>Obtenir la météo du point de départ</strong>
              <small>
                J’accepte l’envoi ponctuel de cette seule coordonnée à
                Open‑Meteo. La décocher puis enregistrer retire le consentement
                et efface la prévision conservée.
              </small>
            </span>
          </label>

          {contextError && (
            <div className="form-alert" role="alert">
              <span aria-hidden="true">!</span>
              <p>{contextError}</p>
            </div>
          )}

          {contextSuccess && (
            <div className="success-alert" role="status">
              <span aria-hidden="true">✓</span>
              <p>{contextSuccess}</p>
            </div>
          )}

          <button
            className="primary-button outdoor-save"
            type="submit"
            disabled={savingContext}
          >
            {savingContext && (
              <span className="button-spinner" aria-hidden="true" />
            )}
            {savingContext ? 'Calcul en cours…' : 'Enregistrer et recalculer'}
          </button>
        </form>

        {outdoorContext && (
          <OutdoorResults context={outdoorContext} />
        )}
      </section>

      <TrackAnalysisPanel analysis={analysis} />

      <section className="report-method">
        <div>
          <p className="auth-kicker">Méthode transparente</p>
          <h2>Comment lire ces nombres</h2>
        </div>
        <p>
          La distance suit l’ellipsoïde WGS84. Les dénivelés utilisent
          uniquement deux altitudes consécutives connues. La lumière est
          calculée localement au premier point du GPX avec le fuseau choisi.
          Avec consentement, les valeurs météo horaires sont agrégées sur la
          durée prévue. Le moteur applique ensuite des seuils versionnés et
          affiche chaque preuve séparément ; il ne calcule aucun score de
          sécurité.
        </p>
      </section>
    </main>
  )
}

function OutdoorResults({ context }: { context: OutdoorContext }) {
  const weather = context.weather

  return (
    <div className="outdoor-results">
      <section className="outdoor-result-panel">
        <div className="outdoor-result-heading">
          <div>
            <p className="auth-kicker">Calcul local</p>
            <h3>Lumière au départ</h3>
          </div>
          <span>{formatPlannedWindow(context)}</span>
        </div>
        <div className="outdoor-result-metrics">
          <ContextMetric
            label="Lever"
            value={formatTime(context.daylight.sunrise, context.timeZone)}
          />
          <ContextMetric
            label="Coucher"
            value={formatTime(context.daylight.sunset, context.timeZone)}
          />
          <ContextMetric
            label="Hors crépuscule civil"
            value={formatDuration(context.daylight.expectedDarknessMinutes)}
          />
        </div>
        <p className="outdoor-caveat">
          Relief, versant, forêt et horizon local peuvent masquer le soleil
          plus tôt que ce calcul astronomique.
        </p>
      </section>

      <section className="outdoor-result-panel">
        <div className="outdoor-result-heading">
          <div>
            <p className="auth-kicker">Prévision ponctuelle</p>
            <h3>Météo au point de départ</h3>
          </div>
          <WeatherStatusBadge status={weather.status} />
        </div>

        {weather.status === 'AVAILABLE' ? (
          <>
            <div className="outdoor-result-metrics weather-metrics">
              <ContextMetric
                label="Température"
                value={formatRange(
                  weather.minimumTemperatureCelsius,
                  weather.maximumTemperatureCelsius,
                  '°C',
                )}
              />
              <ContextMetric
                label="Ressenti"
                value={formatRange(
                  weather.minimumApparentCelsius,
                  weather.maximumApparentCelsius,
                  '°C',
                )}
              />
              <ContextMetric
                label="Précipitations"
                value={formatPrecipitation(weather)}
              />
              <ContextMetric
                label="Rafales max"
                value={formatUnit(
                  weather.maximumWindGustKilometersPerHour,
                  'km/h',
                )}
              />
            </div>
            <p className="outdoor-caveat">
              Modèle à environ {formatUnit(weather.modelElevationMeters, 'm')}
              {' · '}récupéré le {formatCompactDate(weather.checkedAt)}.
              Cette valeur ne décrit ni une crête ni l’ensemble du parcours.
            </p>
          </>
        ) : (
          <p className="weather-unavailable" role="status">
            {weatherStatusExplanation(weather.status)}
          </p>
        )}

        <p className="weather-source">
          Source :{' '}
          <a
            href={weather.attributionUrl}
            target="_blank"
            rel="noreferrer"
          >
            {weather.source}
          </a>
          {' · '}données sous attribution CC BY 4.0
        </p>
      </section>
    </div>
  )
}

function ReportMetric({
  label,
  value,
  detail,
}: {
  label: string
  value: string
  detail: string
}) {
  return (
    <article>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  )
}

function ContextMetric({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function WeatherStatusBadge({
  status,
}: {
  status: WeatherContext['status']
}) {
  const labels: Record<WeatherContext['status'], string> = {
    AVAILABLE: 'Disponible',
    NOT_REQUESTED: 'Non demandée',
    OUTSIDE_FORECAST_HORIZON: 'Trop tôt',
    UNAVAILABLE: 'Indisponible',
  }

  return (
    <span className={`weather-status weather-status-${status.toLowerCase()}`}>
      {labels[status]}
    </span>
  )
}

function weatherStatusExplanation(
  status: WeatherContext['status'],
): string {
  if (status === 'NOT_REQUESTED') {
    return 'Aucune coordonnée n’a été transmise. Le calcul de lumière reste disponible localement.'
  }

  if (status === 'OUTSIDE_FORECAST_HORIZON') {
    return 'La date est au-delà de l’horizon de prévision. Enregistre à nouveau le contexte à l’approche du départ.'
  }

  return 'La source météo n’a pas répondu correctement. L’horaire reste enregistré ; vérifie une source locale avant toute décision.'
}

function formatPlannedWindow(context: OutdoorContext): string {
  const start = formatDateAtZone(context.plannedStartAt, context.timeZone)
  const end = formatTime(context.plannedEndAt, context.timeZone)

  return `${start} → ${end}`
}

function formatReportDate(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'date inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(date)
}

function formatCompactDate(value: string | null): string {
  if (!value) {
    return 'date inconnue'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'date inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function formatDateAtZone(value: string, timeZone: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'date inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone,
  }).format(date)
}

function formatTime(value: string | null, timeZone: string): string {
  if (!value) {
    return 'Non applicable'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone,
  }).format(date)
}

function formatDuration(minutes: number): string {
  if (minutes === 0) {
    return '0 min'
  }

  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60

  if (hours === 0) {
    return `${remainingMinutes} min`
  }

  return remainingMinutes === 0
    ? `${hours} h`
    : `${hours} h ${remainingMinutes.toString().padStart(2, '0')}`
}

function formatRange(
  minimum: number | null,
  maximum: number | null,
  unit: string,
): string {
  if (minimum === null || maximum === null) {
    return 'Non disponible'
  }

  const formatter = new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 1,
  })

  return `${formatter.format(minimum)}–${formatter.format(maximum)} ${unit}`
}

function formatUnit(value: number | null, unit: string): string {
  if (value === null) {
    return 'Non disponible'
  }

  return `${new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 1,
  }).format(value)} ${unit}`
}

function formatPrecipitation(weather: WeatherContext): string {
  const amount = formatUnit(weather.precipitationSumMillimeters, 'mm')
  const probability = weather.maximumPrecipitationProbabilityPercent

  return probability === null
    ? amount
    : `${amount} · ${probability} % max`
}

function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Paris'
}

function defaultPlannedStart(): string {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  date.setHours(8, 0, 0, 0)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}T08:00`
}

function normalizeLocalDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}

function messageForContextError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return 'Le contexte n’a pas pu être enregistré. Vérifie ta connexion puis réessaie.'
}
