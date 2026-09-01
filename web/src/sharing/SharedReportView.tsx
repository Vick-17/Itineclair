import { TrackAnalysisPanel } from '../track/TrackAnalysisPanel'
import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatGrade,
  formatMeters,
} from '../track/track-format'
import type { SharedTrackReport } from './sharing-api'
import type {
  OutdoorContext,
  WeatherContext,
} from '../track/tracks-api'

export function SharedReportView({
  report,
  preview = false,
}: {
  report: SharedTrackReport
  preview?: boolean
}) {
  const facts = report.track.facts

  return (
    <article className="track-report shared-report-view">
      <header className="report-heading">
        <div>
          <p className="eyebrow">
            <span aria-hidden="true">●</span>
            {preview ? 'Aperçu exact avant partage' : 'Rapport privé partagé'}
          </p>
          <h1>Rapport d’itinéraire</h1>
          <p>{formatCoverage(report.track)}</p>
        </div>
        {report.expiresAt && (
          <time dateTime={report.expiresAt}>
            Expire le {formatDate(report.expiresAt)}
          </time>
        )}
      </header>

      <aside className="report-caution" aria-label="Limite du rapport">
        <span aria-hidden="true">i</span>
        <div>
          <strong>Des faits de préparation, jamais un feu vert.</strong>
          <p>
            Cette vue ne contient ni carte ni position précise. Elle ne
            remplace pas les sources officielles, l’expérience et la décision
            humaine sur le terrain.
          </p>
        </div>
      </aside>

      {facts ? (
        <section className="report-metrics" aria-label="Faits partagés">
          <SharedMetric
            label="Distance"
            value={formatDistance(facts.distanceMeters)}
            detail="Somme WGS84, segments séparés"
          />
          <SharedMetric
            label="Dénivelé positif"
            value={formatMeters(facts.elevationGainMeters)}
            detail={report.track.elevationComplete
              ? 'Altitude complète'
              : 'Valeur partielle'}
          />
          <SharedMetric
            label="Dénivelé négatif"
            value={formatMeters(facts.elevationLossMeters)}
            detail={report.track.elevationComplete
              ? 'Altitude complète'
              : 'Valeur partielle'}
          />
          <SharedMetric
            label="Plage d’altitude"
            value={formatElevationRange(facts)}
            detail="Minimum–maximum du GPX"
          />
          <SharedMetric
            label="Pente montante max"
            value={formatGrade(facts.maximumUphillGradePercent, '+')}
            detail={`Fenêtre ≥ ${facts.gradeMinimumRunMeters} m`}
          />
          <SharedMetric
            label="Pente descendante max"
            value={formatGrade(facts.maximumDownhillGradePercent, '−')}
            detail={`Fenêtre ≥ ${facts.gradeMinimumRunMeters} m`}
          />
        </section>
      ) : (
        <section className="report-unavailable" role="status">
          <strong>Faits non disponibles</strong>
          <p>Le rapport partagé ne fabrique aucune valeur manquante.</p>
        </section>
      )}

      {report.outdoorContext ? (
        <section className="shared-outdoor" aria-labelledby="shared-outdoor-title">
          <div className="outdoor-heading">
            <div>
              <p className="auth-kicker">Date, lumière et météo</p>
              <h2 id="shared-outdoor-title">Contexte prévu</h2>
            </div>
          </div>
          <SharedOutdoorResults context={report.outdoorContext} />
        </section>
      ) : (
        <section className="report-unavailable" role="status">
          <strong>Aucun horaire planifié</strong>
          <p>La lumière et la météo ne sont donc pas interprétées.</p>
        </section>
      )}

      <TrackAnalysisPanel analysis={report.analysis} />

      <section className="shared-privacy" aria-labelledby="shared-privacy-title">
        <p className="auth-kicker">Confidentialité</p>
        <h2 id="shared-privacy-title">Données volontairement absentes</h2>
        <ul>
          {report.privacy.excludedData.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
    </article>
  )
}

function SharedOutdoorResults({ context }: { context: OutdoorContext }) {
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
          <SharedContextMetric
            label="Lever"
            value={formatTime(context.daylight.sunrise, context.timeZone)}
          />
          <SharedContextMetric
            label="Coucher"
            value={formatTime(context.daylight.sunset, context.timeZone)}
          />
          <SharedContextMetric
            label="Hors crépuscule civil"
            value={formatMinutes(context.daylight.expectedDarknessMinutes)}
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
          <span className={`weather-status weather-status-${weather.status.toLowerCase()}`}>
            {weatherStatusLabel(weather.status)}
          </span>
        </div>

        {weather.status === 'AVAILABLE' ? (
          <>
            <div className="outdoor-result-metrics weather-metrics">
              <SharedContextMetric
                label="Température"
                value={formatRange(
                  weather.minimumTemperatureCelsius,
                  weather.maximumTemperatureCelsius,
                  '°C',
                )}
              />
              <SharedContextMetric
                label="Ressenti"
                value={formatRange(
                  weather.minimumApparentCelsius,
                  weather.maximumApparentCelsius,
                  '°C',
                )}
              />
              <SharedContextMetric
                label="Précipitations"
                value={formatPrecipitation(weather)}
              />
              <SharedContextMetric
                label="Rafales max"
                value={formatUnit(
                  weather.maximumWindGustKilometersPerHour,
                  'km/h',
                )}
              />
            </div>
            <p className="outdoor-caveat">
              Altitude du modèle : {formatUnit(
                weather.modelElevationMeters,
                'm',
              )}. Cette valeur ne décrit ni une crête ni tout le parcours.
            </p>
          </>
        ) : (
          <p className="weather-unavailable" role="status">
            La prévision n’est pas disponible ; aucune bonne condition n’est
            déduite de cette absence.
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
        </p>
      </section>
    </div>
  )
}

function SharedContextMetric({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return <div><span>{label}</span><strong>{value}</strong></div>
}

function SharedMetric({
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

function formatDate(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'date inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(date)
}

function formatPlannedWindow(context: OutdoorContext): string {
  const start = new Date(context.plannedStartAt)
  const end = new Date(context.plannedEndAt)

  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return 'Horaire inconnu'
  }

  const dateFormatter = new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: context.timeZone,
  })
  const timeFormatter = new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: context.timeZone,
  })
  return `${dateFormatter.format(start)} → ${timeFormatter.format(end)}`
}

function formatTime(value: string | null, timeZone: string): string {
  if (!value) {
    return 'Non applicable'
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? 'Inconnue'
    : new Intl.DateTimeFormat('fr-FR', {
        hour: '2-digit',
        minute: '2-digit',
        timeZone,
      }).format(date)
}

function formatMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) return `${rest} min`
  return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`
}

function formatRange(
  minimum: number | null,
  maximum: number | null,
  unit: string,
): string {
  if (minimum === null || maximum === null) return 'Non disponible'
  const formatter = new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 1,
  })
  return `${formatter.format(minimum)}–${formatter.format(maximum)} ${unit}`
}

function formatUnit(value: number | null, unit: string): string {
  if (value === null) return 'Non disponible'
  return `${new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 1,
  }).format(value)} ${unit}`
}

function formatPrecipitation(weather: WeatherContext): string {
  const amount = formatUnit(weather.precipitationSumMillimeters, 'mm')
  return weather.maximumPrecipitationProbabilityPercent === null
    ? amount
    : `${amount} · ${weather.maximumPrecipitationProbabilityPercent} % max`
}

function weatherStatusLabel(status: WeatherContext['status']): string {
  const labels: Record<WeatherContext['status'], string> = {
    AVAILABLE: 'Disponible',
    NOT_REQUESTED: 'Non demandée',
    OUTSIDE_FORECAST_HORIZON: 'Trop tôt',
    UNAVAILABLE: 'Indisponible',
  }
  return labels[status]
}
