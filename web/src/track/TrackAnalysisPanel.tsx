import type {
  AnalysisCategory,
  AnalysisEvidence,
  AnalysisSeverity,
  ChecklistStatus,
  TrackAnalysis,
} from './tracks-api'

export function TrackAnalysisPanel({
  analysis,
}: {
  analysis: TrackAnalysis
}) {
  return (
    <section className="rule-analysis" aria-labelledby="analysis-title">
      <div className="rule-analysis-heading">
        <div>
          <p className="auth-kicker">Règles déterministes · version {analysis.ruleSetVersion}</p>
          <h2 id="analysis-title">Signaux à examiner</h2>
          <p>
            Chaque signal montre le fait observé et le seuil interne qui l’a
            déclenché. Les dimensions restent séparées : aucun score global
            ne transforme ces données en verdict.
          </p>
        </div>
        <span className="rule-review-badge">
          Prototype · relecture experte requise
        </span>
      </div>

      {analysis.findings.length === 0 ? (
        <div className="analysis-empty" role="status">
          <strong>Aucun seuil interne déclenché</strong>
          <p>
            Cela ne permet pas de conclure sur les conditions réelles. Les
            contrôles ci-dessous restent nécessaires.
          </p>
        </div>
      ) : (
        <ol className="analysis-findings">
          {analysis.findings.map((finding) => (
            <li
              className={`analysis-finding analysis-finding-${finding.severity.toLowerCase()}`}
              key={finding.code}
            >
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
              <p>{finding.explanation}</p>
              {finding.evidence.length > 0 && (
                <ul className="analysis-evidence" aria-label="Preuves de la règle">
                  {finding.evidence.map((evidence) => (
                    <li key={evidence.metric}>
                      <strong>{evidence.label}</strong>
                      <span>{formatEvidence(evidence)}</span>
                    </li>
                  ))}
                </ul>
              )}
              <p className="analysis-action">
                <strong>À faire :</strong> {finding.action}
              </p>
            </li>
          ))}
        </ol>
      )}

      <div className="analysis-follow-up">
        <section aria-labelledby="analysis-checklist-title">
          <p className="auth-kicker">Checklist de décision</p>
          <h3 id="analysis-checklist-title">Ce qui est présent ou à vérifier</h3>
          <ul className="analysis-checklist">
            {analysis.checklist.map((item) => (
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

        <section className="analysis-limitations" aria-labelledby="analysis-limits-title">
          <p className="auth-kicker">Limites connues</p>
          <h3 id="analysis-limits-title">Ce que le moteur ne conclut pas</h3>
          <ul>
            {analysis.limitations.map((limitation) => (
              <li key={limitation}>{limitation}</li>
            ))}
          </ul>
          <p className="analysis-generated-at">
            Analyse calculée le {formatDate(analysis.generatedAt)}.
          </p>
        </section>
      </div>
    </section>
  )
}

function categoryLabel(category: AnalysisCategory): string {
  const labels: Record<AnalysisCategory, string> = {
    DATA_QUALITY: 'Qualité des données',
    PHYSICAL_LOAD: 'Charge physique',
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
    PARTIAL: 'Couverture partielle',
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
