import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatMaximumGrades,
  formatMeters,
} from '../track/track-format'
import type { Track } from '../track/tracks-api'

export function RouteCheckStep({
  track,
  onBack,
  onContinue,
}: {
  track: Track
  onBack: () => void
  onContinue: () => void
}) {
  const facts = track.facts

  return (
    <section
      className="preparation-page route-check-step"
      aria-labelledby="route-check-title"
    >
      <PreparationBackButton onClick={onBack}>
        Mes sorties
      </PreparationBackButton>

      <header className="preparation-heading">
        <p className="workspace-kicker">Étape 1 sur 3 · La trace</p>
        <h1 id="route-check-title">Vérifie que c’est le bon parcours.</h1>
        <p>
          Compare ces quatre repères avec la source de ton GPX avant
          d’ajouter une date. Une erreur de fichier se corrige plus facilement
          maintenant.
        </p>
      </header>

      <div className="route-check-identity">
        <div>
          <span>Trace sélectionnée</span>
          <strong>{track.name}</strong>
          <small>{track.sourceFilename}</small>
        </div>
        <p>{formatCoverage(track)}</p>
      </div>

      {facts ? (
        <dl className="route-check-facts">
          <RouteFact
            number="01"
            label="Distance"
            value={formatDistance(facts.distanceMeters)}
          />
          <RouteFact
            number="02"
            label="Montée"
            value={formatMeters(facts.elevationGainMeters)}
          />
          <RouteFact
            number="03"
            label="Altitudes"
            value={formatElevationRange(facts)}
          />
          <RouteFact
            number="04"
            label="Pentes maximales"
            value={formatMaximumGrades(facts)}
            detail={`calculées sur au moins ${facts.gradeMinimumRunMeters} m`}
          />
        </dl>
      ) : (
        <div className="preparation-blocker" role="status">
          <strong>Les faits de la trace ne sont pas encore disponibles.</strong>
          <p>
            Reviens à Mes sorties puis rouvre la trace. La préparation ne peut
            pas continuer sans ces données de base.
          </p>
        </div>
      )}

      {facts && !track.elevationComplete && (
        <aside className="preparation-note" aria-label="Altitude incomplète">
          <span aria-hidden="true">!</span>
          <p>
            <strong>
              Altitude {track.elevationPointCount === 0 ? 'absente' : 'partielle'}.
            </strong>
            {' '}Le dénivelé, la plage d’altitude et les pentes peuvent être
            incomplets. Garde la source du parcours comme référence.
          </p>
        </aside>
      )}

      <div className="preparation-actions">
        <button
          className="workspace-primary-action"
          type="button"
          onClick={onContinue}
          disabled={!facts}
        >
          Cette trace me convient
          <span aria-hidden="true">→</span>
        </button>
        <button
          className="preparation-secondary-action"
          type="button"
          onClick={onBack}
        >
          Choisir une autre trace
        </button>
      </div>
    </section>
  )
}

function RouteFact({
  number,
  label,
  value,
  detail,
}: {
  number: string
  label: string
  value: string
  detail?: string
}) {
  return (
    <div>
      <dt>
        <span aria-hidden="true">{number}</span>
        {label}
      </dt>
      <dd>{value}</dd>
      {detail && <small>{detail}</small>}
    </div>
  )
}

function PreparationBackButton({
  children,
  onClick,
}: {
  children: string
  onClick: () => void
}) {
  return (
    <button
      className="preparation-back"
      type="button"
      onClick={onClick}
    >
      <span aria-hidden="true">←</span>
      {children}
    </button>
  )
}
