import {
  prioritizeChecklist,
  prioritizeFindings,
} from './report-hierarchy'
import type {
  AnalysisCategory,
  AnalysisEvidence,
  AnalysisSeverity,
  ChecklistStatus,
  TrackAnalysis,
} from './tracks-api'

export function TrackAnalysisPanel({
  analysis,
  idPrefix,
}: {
  analysis: TrackAnalysis
  idPrefix: string
}) {
  const findings = prioritizeFindings(analysis.findings)
  const checklist = prioritizeChecklist(analysis.checklist)
  const remainingCheckCount = checklist.filter(
    (item) => item.status !== 'AVAILABLE',
  ).length
  const titleId = `${idPrefix}-title`
  const checklistTitleId = `${idPrefix}-checklist-title`

  return (
    <section className="rule-analysis" aria-labelledby={titleId}>
      <div className="rule-analysis-heading">
        <div>
          <p className="auth-kicker">Comprendre les points signalés</p>
          <h2 id={titleId}>Pourquoi ces éléments demandent ton attention</h2>
          <p>
            Les actions sont affichées avant les détails du calcul. Ouvre les
            preuves seulement si tu veux comprendre le seuil utilisé.
          </p>
        </div>
        <span className="rule-review-badge">
          Prototype · relecture experte requise
        </span>
      </div>

      {findings.length === 0 ? (
        <div className="analysis-empty" role="status">
          <strong>Aucun seuil automatique déclenché</strong>
          <p>
            Ce résultat ne décrit pas toutes les conditions réelles. Termine
            les vérifications ci-dessous avant de prendre ta décision.
          </p>
        </div>
      ) : (
        <ol className="analysis-findings">
          {findings.map((finding, index) => (
            <li
              className={`analysis-finding analysis-finding-${finding.severity.toLowerCase()}`}
              key={finding.code}
            >
              <div className="analysis-finding-index" aria-hidden="true">
                {String(index + 1).padStart(2, '0')}
              </div>
              <div className="analysis-finding-content">
                <div className="analysis-finding-topline">
                  <span className="analysis-category">
                    {categoryLabel(finding.category)}
                  </span>
                  <span className="analysis-severity">
                    {severityIcon(finding.severity)}{' '}
                    {severityLabel(finding.severity)}
                  </span>
                </div>
                <h3>{finding.title}</h3>
                <p className="analysis-action">
                  <strong>Action proposée</strong>
                  <span>{finding.action}</span>
                </p>
                <p className="analysis-explanation">{finding.explanation}</p>
                {finding.evidence.length > 0 && (
                  <details className="analysis-evidence-details">
                    <summary>Pourquoi ce point apparaît</summary>
                    <ul
                      className="analysis-evidence"
                      aria-label={`Données utilisées pour ${finding.title}`}
                    >
                      {finding.evidence.map((evidence) => (
                        <li key={evidence.metric}>
                          <strong>{evidence.label}</strong>
                          <span>{formatEvidence(evidence)}</span>
                        </li>
                      ))}
                    </ul>
                  </details>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}

      <section
        className="analysis-checks"
        aria-labelledby={checklistTitleId}
      >
        <div className="analysis-subheading">
          <p className="auth-kicker">Vérifications avant de décider</p>
          <h3 id={checklistTitleId}>Ce qu’Itinéclair connaît — et ce qui manque</h3>
          <p>
            {remainingCheckCount === 0
              ? 'Les données prévues par cette checklist sont présentes, mais elles peuvent évoluer et ne couvrent pas tout le terrain.'
              : `${remainingCheckCount} ${remainingCheckCount === 1 ? 'élément reste' : 'éléments restent'} à vérifier ou à compléter avec une source locale.`}
          </p>
        </div>

        <ul className="analysis-checklist">
          {checklist.map((item) => (
            <li key={item.code}>
              <span
                className={`analysis-check-status analysis-check-status-${item.status.toLowerCase()}`}
                aria-hidden="true"
              >
                {checklistIcon(item.status)}
              </span>
              <div>
                <strong>{item.title}</strong>
                <p>{item.detail}</p>
              </div>
              <small>{checklistLabel(item.status)}</small>
            </li>
          ))}
        </ul>
      </section>

      <details className="analysis-transparency">
        <summary>Limites, version et date de cette analyse</summary>
        <div className="analysis-transparency-content">
          <div>
            <h3>Ce que le moteur ne conclut pas</h3>
            <ul>
              {analysis.limitations.map((limitation) => (
                <li key={limitation}>{limitation}</li>
              ))}
            </ul>
          </div>
          <p className="analysis-generated-at">
            Règles version {analysis.ruleSetVersion} · analyse calculée le{' '}
            {formatDate(analysis.generatedAt)}.
          </p>
        </div>
      </details>
    </section>
  )
}

function categoryLabel(category: AnalysisCategory): string {
  const labels: Record<AnalysisCategory, string> = {
    DATA_QUALITY: 'Qualité des données',
    PHYSICAL_LOAD: 'Effort physique',
    ROUTE_CHARACTERISTICS: 'Caractéristiques du parcours',
    LIGHT: 'Lumière',
    WEATHER: 'Conditions météo',
  }

  return labels[category]
}

function severityLabel(severity: AnalysisSeverity): string {
  const labels: Record<AnalysisSeverity, string> = {
    NOTICE: 'À compléter',
    CAUTION: 'Point à examiner',
    STRONG_CAUTION: 'Attention renforcée',
  }

  return labels[severity]
}

function severityIcon(severity: AnalysisSeverity): string {
  return severity === 'NOTICE' ? 'i' : '!'
}

function checklistLabel(status: ChecklistStatus): string {
  const labels: Record<ChecklistStatus, string> = {
    AVAILABLE: 'Donnée présente',
    PARTIAL: 'À compléter',
    TO_VERIFY: 'À vérifier',
  }

  return labels[status]
}

function checklistIcon(status: ChecklistStatus): string {
  if (status === 'AVAILABLE') {
    return '✓'
  }

  return status === 'PARTIAL' ? '◐' : '○'
}

function formatEvidence(evidence: AnalysisEvidence): string {
  const formatter = new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 1,
  })
  const symbols: Record<AnalysisEvidence['comparison'], string> = {
    GREATER_THAN: '>',
    GREATER_OR_EQUAL: '≥',
    LESS_THAN: '<',
    LESS_OR_EQUAL: '≤',
  }

  return `${formatter.format(evidence.observedValue)} ${evidence.unit} observés · seuil ${symbols[evidence.comparison]} ${formatter.format(evidence.thresholdValue)} ${evidence.unit}`
}

function formatDate(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'date inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
