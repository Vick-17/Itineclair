import {
  useState,
  type FormEvent,
} from 'react'

import { ApiError } from '../api/api-client'
import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatGrade,
  formatMeters,
} from './track-format'
import {
  saveOutdoorContext,
  type OutdoorContext,
  type Track,
  type WeatherContext,
} from './tracks-api'

export function TrackReport({
  track,
  outdoorContext,
  onOutdoorContextChange,
  onUnauthorized,
  onBack,
}: {
  track: Track
  outdoorContext: OutdoorContext | null
  onOutdoorContextChange: (context: OutdoorContext) => void
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
      setContextSuccess(
        weatherConsent
          ? 'Horaire enregistré et prévision météo actualisée.'
          : 'Horaire enregistré. Aucun point GPS n’a été transmis.',
      )
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
        <span>Rapport factuel · version 2</span>
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

      <div className="report-columns">
        <section className="report-panel">
          <p className="auth-kicker">Ce que nous savons</p>
          <h2>Qualité des données de la trace</h2>
          <ul className="report-fact-list">
            <ReportFact
              status="known"
              title="Structure GPX vérifiée"
              detail={`${track.pointCount.toLocaleString('fr-FR')} points répartis dans ${track.segmentCount} segment${track.segmentCount > 1 ? 's' : ''}.`}
            />
            <ReportFact
              status={track.elevationComplete ? 'known' : 'attention'}
              title={track.elevationComplete ? 'Altitude complète' : 'Altitude incomplète'}
              detail={
                track.elevationComplete
                  ? 'Chaque point contient une altitude.'
                  : `${track.elevationPointCount.toLocaleString('fr-FR')} points sur ${track.pointCount.toLocaleString('fr-FR')} contiennent une altitude ; D+ et D− sont donc partiels.`
              }
            />
            <ReportFact
              status="known"
              title="Ruptures de segment respectées"
              detail="Aucune distance ni pente n’est inventée entre deux segments."
            />
          </ul>
        </section>

        <section className="report-panel">
          <p className="auth-kicker">Avant de décider</p>
          <h2>Contexte à vérifier</h2>
          <ul className="report-check-list">
            <ReportCheck
              complete={outdoorContext?.weather.status === 'AVAILABLE'}
              title={weatherChecklistTitle(outdoorContext?.weather)}
              detail={weatherChecklistDetail(outdoorContext?.weather)}
            />
            <ReportCheck
              complete={outdoorContext !== null}
              title="Lumière et horaire"
              detail={daylightChecklistDetail(outdoorContext)}
            />
            <ReportCheck
              complete={false}
              title="État du terrain et passages exposés"
              detail="À vérifier avec une source locale récente"
            />
            <ReportCheck
              complete={false}
              title="Niveau, matériel et composition du groupe"
              detail="À évaluer avant le départ"
            />
          </ul>
          <p className="report-next-step">
            Une prévision au point de départ peut différer fortement des
            conditions en altitude, sur une crête ou dans une autre vallée.
          </p>
        </section>
      </div>

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
          durée prévue ; aucun jugement de sécurité n’en est déduit.
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

function ReportFact({
  status,
  title,
  detail,
}: {
  status: 'known' | 'attention'
  title: string
  detail: string
}) {
  return (
    <li>
      <span
        className={`report-status report-status-${status}`}
        aria-hidden="true"
      >
        {status === 'known' ? '✓' : '!'}
      </span>
      <div>
        <strong>{title}</strong>
        <p>{detail}</p>
      </div>
    </li>
  )
}

function ReportCheck({
  complete,
  title,
  detail,
}: {
  complete: boolean
  title: string
  detail: string
}) {
  return (
    <li>
      <span aria-hidden="true">{complete ? '✓' : '○'}</span>
      <strong>{title}</strong>
      <small>{detail}</small>
    </li>
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

function weatherChecklistTitle(weather?: WeatherContext): string {
  if (weather?.status === 'AVAILABLE') {
    return 'Prévision au départ consultée'
  }

  if (weather?.status === 'OUTSIDE_FORECAST_HORIZON') {
    return 'Prévision pas encore disponible'
  }

  if (weather?.status === 'UNAVAILABLE') {
    return 'Service météo indisponible'
  }

  return 'Météo locale et évolution'
}

function weatherChecklistDetail(weather?: WeatherContext): string {
  if (weather?.status === 'AVAILABLE') {
    return `Récupérée le ${formatCompactDate(weather.checkedAt)}`
  }

  if (weather?.status === 'NOT_REQUESTED') {
    return 'Aucune coordonnée transmise'
  }

  if (weather?.status === 'OUTSIDE_FORECAST_HORIZON') {
    return 'À actualiser à l’approche du départ'
  }

  return 'À réessayer et à vérifier ailleurs'
}

function daylightChecklistDetail(
  context: OutdoorContext | null,
): string {
  if (!context) {
    return 'Pas encore calculé'
  }

  const darkness = context.daylight.expectedDarknessMinutes

  return darkness === 0
    ? 'Sortie prévue dans le crépuscule civil calculé'
    : `${formatDuration(darkness)} prévues hors crépuscule civil`
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
