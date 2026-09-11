import {
  formatDistance,
  formatMeters,
} from '../track/track-format'
import type { Track } from '../track/tracks-api'

export function WorkspaceHome({
  recentTrack,
  loading,
  openingTrackId,
  onPrepare,
  onOpenReport,
  onViewOutings,
}: {
  recentTrack: Track | null
  loading: boolean
  openingTrackId: string | null
  onPrepare: () => void
  onOpenReport: (trackId: string) => void
  onViewOutings: () => void
}) {
  return (
    <section
      className="workspace-page workspace-home"
      aria-labelledby="workspace-home-title"
    >
      <header className="workspace-intro">
        <p className="workspace-kicker">Ta prochaine étape</p>
        <h1 id="workspace-home-title">
          Préparer une sortie, sans chercher où commencer.
        </h1>
        <p>
          Ajoute une trace, indique quand tu pars, puis lis les points à
          vérifier. Itinéclair rassemble les faits, pas la décision.
        </p>
      </header>

      <section
        className="workspace-start"
        aria-labelledby="workspace-start-title"
      >
        <div>
          <span className="workspace-action-number" aria-hidden="true">
            01
          </span>
          <h2 id="workspace-start-title">Commencer une préparation</h2>
          <p>
            Il te faut seulement un fichier GPX. La date et les conditions
            seront ajoutées ensuite.
          </p>
        </div>

        <button
          className="workspace-primary-action"
          type="button"
          onClick={onPrepare}
        >
          Préparer une sortie
          <span aria-hidden="true">→</span>
        </button>
      </section>

      <ol className="workspace-steps" aria-label="Étapes de préparation">
        <li>
          <span>01</span>
          <strong>Choisir la trace</strong>
          <p>Un fichier GPX téléchargé depuis ta source habituelle.</p>
        </li>
        <li>
          <span>02</span>
          <strong>Indiquer le départ</strong>
          <p>La date, l’heure et la durée que tu prévois.</p>
        </li>
        <li>
          <span>03</span>
          <strong>Lire la préparation</strong>
          <p>Les points d’attention, les faits et la checklist.</p>
        </li>
      </ol>

      <section
        className="workspace-resume"
        aria-labelledby="workspace-resume-title"
      >
        <div className="workspace-section-heading">
          <div>
            <p className="workspace-kicker">Reprendre simplement</p>
            <h2 id="workspace-resume-title">Dernière trace</h2>
          </div>

          <button
            className="workspace-text-action"
            type="button"
            onClick={onViewOutings}
          >
            Voir toutes mes sorties
          </button>
        </div>

        {loading && (
          <p className="workspace-status" aria-live="polite">
            Chargement de ta dernière trace…
          </p>
        )}

        {!loading && !recentTrack && (
          <div className="workspace-status">
            <strong>Aucune trace pour le moment</strong>
            <p>Commence par préparer ta première sortie.</p>
          </div>
        )}

        {recentTrack && (
          <article className="workspace-recent-track">
            <div>
              <time dateTime={recentTrack.createdAt}>
                Importée {formatImportDate(recentTrack.createdAt)}
              </time>
              <h3>{recentTrack.name}</h3>
              <p>{formatTrackSummary(recentTrack)}</p>
            </div>

            <button
              className="workspace-row-action"
              type="button"
              onClick={() => onOpenReport(recentTrack.id)}
              disabled={openingTrackId !== null}
              aria-label={`Voir la préparation de ${recentTrack.name}`}
            >
              {openingTrackId === recentTrack.id
                ? 'Ouverture…'
                : 'Voir la préparation'}
              <span aria-hidden="true">→</span>
            </button>
          </article>
        )}
      </section>

      <p className="workspace-safety-reminder">
        <span aria-hidden="true">!</span>
        Itinéclair aide à préparer. Les conditions réelles, les sources
        officielles et ton observation du terrain restent prioritaires.
      </p>
    </section>
  )
}

function formatTrackSummary(track: Track): string {
  if (!track.facts) {
    return `${track.pointCount.toLocaleString('fr-FR')} points enregistrés`
  }

  return [
    formatDistance(track.facts.distanceMeters),
    `${formatMeters(track.facts.elevationGainMeters)} de montée`,
  ].join(' · ')
}

function formatImportDate(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'à une date inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
  }).format(date)
}
