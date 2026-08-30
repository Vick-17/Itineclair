import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatGrade,
  formatMeters,
} from './track-format'
import type { Track } from './tracks-api'

export function TrackReport({
  track,
  onBack,
}: {
  track: Track
  onBack: () => void
}) {
  const facts = track.facts

  return (
    <main className="track-report">
      <nav className="report-navigation" aria-label="Navigation du rapport">
        <button type="button" onClick={onBack}>
          <span aria-hidden="true">←</span>
          Mes traces
        </button>
        <span>Rapport factuel · version 1</span>
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
          <strong>Un rapport de faits, pas un feu vert.</strong>
          <p>
            Ces données décrivent le fichier GPX. Elles ne disent pas si la
            sortie est sûre aujourd’hui et ne remplacent ni la météo, ni une
            carte adaptée, ni l’expérience du terrain.
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
          <h2>Contexte encore à vérifier</h2>
          <ul className="report-check-list">
            <ReportCheck title="Météo locale et évolution" />
            <ReportCheck title="Lumière, horaire et marge de retour" />
            <ReportCheck title="État du terrain et passages exposés" />
            <ReportCheck title="Niveau, matériel et composition du groupe" />
          </ul>
          <p className="report-next-step">
            Ces vérifications seront enrichies dans les prochaines tranches
            météo, lumière et règles explicables.
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
          uniquement deux altitudes consécutives connues. Les pentes maximales
          emploient une fenêtre d’au moins 25 mètres afin de réduire les pics
          créés par des points presque superposés. Aucun lissage ni modèle
          numérique de terrain n’est appliqué dans cette version.
        </p>
      </section>
    </main>
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

function ReportCheck({ title }: { title: string }) {
  return (
    <li>
      <span aria-hidden="true">○</span>
      <strong>{title}</strong>
      <small>Pas encore analysé</small>
    </li>
  )
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
