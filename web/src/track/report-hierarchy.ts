import type {
  AnalysisChecklistItem,
  AnalysisFinding,
  AnalysisSeverity,
  ChecklistStatus,
  TrackAnalysis,
} from './tracks-api.ts'

const severityPriority: Record<AnalysisSeverity, number> = {
  STRONG_CAUTION: 0,
  CAUTION: 1,
  NOTICE: 2,
}

const checklistPriority: Record<ChecklistStatus, number> = {
  TO_VERIFY: 0,
  PARTIAL: 1,
  AVAILABLE: 2,
}

export type ReportAttentionTone = 'strong' | 'review' | 'neutral'

export type ReportSummary = {
  tone: ReportAttentionTone
  title: string
  description: string
  findingCount: number
  remainingCheckCount: number
}

export function prioritizeFindings(
  findings: readonly AnalysisFinding[],
): AnalysisFinding[] {
  return [...findings].sort(
    (left, right) =>
      severityPriority[left.severity] - severityPriority[right.severity],
  )
}

export function prioritizeChecklist(
  checklist: readonly AnalysisChecklistItem[],
): AnalysisChecklistItem[] {
  return [...checklist].sort(
    (left, right) =>
      checklistPriority[left.status] - checklistPriority[right.status],
  )
}

export function buildReportSummary(analysis: TrackAnalysis): ReportSummary {
  const findingCount = analysis.findings.length
  const strongFindingCount = analysis.findings.filter(
    (finding) => finding.severity === 'STRONG_CAUTION',
  ).length
  const remainingCheckCount = analysis.checklist.filter(
    (item) => item.status !== 'AVAILABLE',
  ).length

  if (strongFindingCount > 0) {
    return {
      tone: 'strong',
      title: countSentence(
        strongFindingCount,
        'point demande une attention renforcée',
        'points demandent une attention renforcée',
      ),
      description: followUpDescription(remainingCheckCount),
      findingCount,
      remainingCheckCount,
    }
  }

  if (findingCount > 0) {
    return {
      tone: 'review',
      title: countSentence(
        findingCount,
        'point est à examiner',
        'points sont à examiner',
      ),
      description: followUpDescription(remainingCheckCount),
      findingCount,
      remainingCheckCount,
    }
  }

  return {
    tone: 'neutral',
    title: 'Aucun seuil automatique déclenché',
    description: remainingCheckCount > 0
      ? `Cela ne signifie pas que la sortie est sûre. ${countSentence(
          remainingCheckCount,
          'vérification reste à faire',
          'vérifications restent à faire',
        )} avant de décider.`
      : 'Cela ne signifie pas que la sortie est sûre. Consulte les limites du calcul et les sources locales avant de décider.',
    findingCount,
    remainingCheckCount,
  }
}

function followUpDescription(remainingCheckCount: number): string {
  if (remainingCheckCount === 0) {
    return 'Commence par les actions proposées, puis consulte les limites du calcul et les sources locales.'
  }

  return `Commence par les actions proposées. ${countSentence(
    remainingCheckCount,
    'vérification reste ensuite à faire',
    'vérifications restent ensuite à faire',
  )}.`
}

function countSentence(
  count: number,
  singular: string,
  plural: string,
): string {
  return `${count} ${count === 1 ? singular : plural}`
}
