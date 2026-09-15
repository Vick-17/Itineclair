import {
  buildReportSummary,
  prioritizeChecklist,
  prioritizeFindings,
} from './report-hierarchy'
import type { TrackAnalysis } from './tracks-api'

export function ReportOverview({
  analysis,
  idPrefix,
  analysisTitleId,
  caution,
}: {
  analysis: TrackAnalysis
  idPrefix: string
  analysisTitleId: string
  caution: string
}) {
  const summary = buildReportSummary(analysis)
  const priorityFindings = prioritizeFindings(analysis.findings).slice(0, 3)
  const remainingChecks = prioritizeChecklist(analysis.checklist)
    .filter((item) => item.status !== 'AVAILABLE')
    .slice(0, 3)
  const titleId = `${idPrefix}-overview-title`

  return (
    <section
      className={`report-overview report-overview-${summary.tone}`}
      aria-labelledby={titleId}
    >
      <div className="report-overview-heading">
        <p className="auth-kicker">À regarder d’abord</p>
        <h2 id={titleId}>{summary.title}</h2>
        <p>{summary.description}</p>
      </div>

      <dl className="report-overview-counts" aria-label="Résumé du rapport">
        <div>
          <dt>Points signalés</dt>
          <dd>{summary.findingCount}</dd>
        </div>
        <div>
          <dt>Vérifications restantes</dt>
          <dd>{summary.remainingCheckCount}</dd>
        </div>
      </dl>

      {priorityFindings.length > 0 ? (
        <ol className="report-priority-list">
          {priorityFindings.map((finding) => (
            <li key={finding.code}>
              <span aria-hidden="true">{priorityIcon(finding.severity)}</span>
              <div>
                <strong>{finding.title}</strong>
                <p>{finding.action}</p>
              </div>
            </li>
          ))}
        </ol>
      ) : remainingChecks.length > 0 ? (
        <ul className="report-priority-list">
          {remainingChecks.map((item) => (
            <li key={item.code}>
              <span aria-hidden="true">○</span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.detail}</p>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <p className="report-overview-empty">
          Le moteur n’a pas trouvé de point dépassant ses seuils. Il ne connaît
          toutefois ni l’état réel du sentier ni les capacités du groupe.
        </p>
      )}

      <div className="report-overview-footer">
        <div className="report-overview-caution">
          <span aria-hidden="true">i</span>
          <p>
            <strong>Ce résumé n’est pas un feu vert.</strong> {caution}
          </p>
        </div>
        <a href={`#${analysisTitleId}`}>Lire tous les détails</a>
      </div>
    </section>
  )
}

function priorityIcon(severity: string): string {
  return severity === 'NOTICE' ? 'i' : '!'
}
