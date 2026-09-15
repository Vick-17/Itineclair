import { TrackAnalysisPanel } from './TrackAnalysisPanel'
import { TrackFeedbackPanel } from './TrackFeedbackPanel'
import { ReportOverview } from './ReportOverview'
import { TrackSharePanel } from '../sharing/TrackSharePanel'
import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatGrade,
  formatMeters,
} from './track-format'
import {
  type OutdoorContext,
  type Track,
  type TrackAnalysis,
  type WeatherContext,
} from './tracks-api'

export function TrackReport({
  track,
  outdoorContext,
  analysis,
  onEditDeparture,
  onUnauthorized,
  onBack,
}: {
  track: Track
  outdoorContext: OutdoorContext | null
  analysis: TrackAnalysis
  onEditDeparture: () => void
  onUnauthorized: () => void
  onBack: () => void
}) {
  const facts = track.facts

  return (
    <article className="track-report">
      <nav className="report-navigation" aria-label="Navigation du rapport">
        <button type="button" onClick={onBack}>
          <span aria-hidden="true">←</span>
          Mes sorties
        </button>
        <span>Préparation explicable · version 3</span>
      </nav>

      <header className="report-heading">
        <div>
          <p className="eyebrow">
            <span aria-hidden="true">●</span>
            Préparation de sortie
          </p>
          <h1>{track.name}</h1>
          <p>{track.sourceFilename} · {formatCoverage(track)}</p>
        </div>
        <time dateTime={track.createdAt}>
          Importée le {formatReportDate(track.createdAt)}
        </time>
      </header>

      <ReportOverview
        analysis={analysis}
        idPrefix="owner"
        analysisTitleId="owner-analysis-title"
        caution="Vérifie les bulletins locaux, les alertes, l’état du terrain et les capacités réelles du groupe."
      />

      <TrackAnalysisPanel
        analysis={analysis}
        idPrefix="owner-analysis"
      />

      <section className="outdoor-context" aria-labelledby="outdoor-title">
        <div className="outdoor-heading">
          <div>
            <p className="auth-kicker">Conditions liées à l’horaire</p>
            <h2 id="outdoor-title">Départ, lumière et météo</h2>
            <p>
              Ces informations décrivent le point de départ et la durée
              prévue, pas l’ensemble du parcours.
            </p>
          </div>
          <div className="outdoor-heading-actions">
            {outdoorContext && (
              <span>
                Mis à jour le {formatCompactDate(outdoorContext.updatedAt)}
              </span>
            )}
            <button type="button" onClick={onEditDeparture}>
              Modifier le départ
            </button>
          </div>
        </div>

        {outdoorContext ? (
          <OutdoorResults context={outdoorContext} />
        ) : (
          <div className="report-unavailable" role="status">
            <strong>Départ non renseigné</strong>
            <p>Ajoute une date et une durée pour calculer la lumière.</p>
          </div>
        )}
      </section>

      <section className="report-route" aria-labelledby="report-route-title">
        <div className="report-section-heading">
          <p className="auth-kicker">Repères du parcours</p>
          <h2 id="report-route-title">La trace en chiffres</h2>
          <p>
            Ces valeurs viennent uniquement du fichier GPX importé. Compare-les
            avec la source originale du parcours.
          </p>
        </div>

        {facts ? (
          <dl className="report-metrics">
            <ReportMetric
              label="Distance"
              value={formatDistance(facts.distanceMeters)}
              detail="Calculée entre les points du fichier"
            />
            <ReportMetric
              label="Dénivelé positif"
              value={formatMeters(facts.elevationGainMeters)}
              detail={track.elevationComplete ? 'Altitude du GPX complète' : 'Valeur partielle'}
            />
            <ReportMetric
              label="Dénivelé négatif"
              value={formatMeters(facts.elevationLossMeters)}
              detail={track.elevationComplete ? 'Altitude du GPX complète' : 'Valeur partielle'}
            />
            <ReportMetric
              label="Plage d’altitude"
              value={formatElevationRange(facts)}
              detail="Point le plus bas → point le plus haut"
            />
            <ReportMetric
              label="Pente montante maximale"
              value={formatGrade(facts.maximumUphillGradePercent, '+')}
              detail={`Mesurée sur au moins ${facts.gradeMinimumRunMeters} m`}
            />
            <ReportMetric
              label="Pente descendante maximale"
              value={formatGrade(facts.maximumDownhillGradePercent, '−')}
              detail={`Mesurée sur au moins ${facts.gradeMinimumRunMeters} m`}
            />
          </dl>
        ) : (
          <div className="report-unavailable" role="status">
            <strong>Calcul des faits en attente</strong>
            <p>Recharge Mes sorties pour relancer l’analyse de cette trace.</p>
          </div>
        )}
      </section>

      <details className="report-method">
        <summary>Comment Itinéclair calcule ces informations</summary>
        <div className="report-method-content">
          <p>
            La distance est calculée entre les coordonnées du GPX en tenant
            compte de la forme de la Terre. Les dénivelés utilisent uniquement
            deux altitudes consécutives connues. La lumière est calculée au
            premier point de la trace avec le fuseau choisi.
          </p>
          <p>
            Avec ton consentement, les prévisions météo horaires sont agrégées
            sur la durée prévue. Le moteur applique ensuite des seuils
            versionnés et conserve chaque dimension séparée. Il ne calcule
            aucun score de sécurité.
          </p>
        </div>
      </details>

      <TrackSharePanel
        trackId={track.id}
        onUnauthorized={onUnauthorized}
      />

      <div className="report-after-outing">
        <TrackFeedbackPanel
          trackId={track.id}
          onUnauthorized={onUnauthorized}
        />
      </div>
    </article>
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
    <div>
      <dt>{label}</dt>
      <dd>
        <strong>{value}</strong>
        <small>{detail}</small>
      </dd>
    </div>
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
