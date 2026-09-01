import type {
  Track,
  TrackFacts,
} from './tracks-api.ts'

export function formatDistance(meters: number): string {
  if (meters < 1_000) {
    return `${Math.round(meters).toLocaleString('fr-FR')} m`
  }

  return `${(meters / 1_000).toLocaleString('fr-FR', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })} km`
}

export function formatMeters(meters: number | null): string {
  if (meters === null) {
    return '—'
  }

  return `${Math.round(meters).toLocaleString('fr-FR')} m`
}

export function formatElevationRange(facts: TrackFacts): string {
  if (
    facts.minimumElevationMeters === null
    || facts.maximumElevationMeters === null
  ) {
    return '—'
  }

  return `${Math.round(facts.minimumElevationMeters).toLocaleString('fr-FR')}–${Math.round(facts.maximumElevationMeters).toLocaleString('fr-FR')} m`
}

export function formatMaximumGrades(facts: TrackFacts): string {
  const grades: string[] = []

  if (facts.maximumUphillGradePercent !== null) {
    grades.push(formatGrade(facts.maximumUphillGradePercent, '+'))
  }

  if (facts.maximumDownhillGradePercent !== null) {
    grades.push(formatGrade(facts.maximumDownhillGradePercent, '−'))
  }

  return grades.length > 0 ? grades.join(' / ') : '—'
}

export function formatGrade(
  gradePercent: number | null,
  direction: '+' | '−',
): string {
  if (gradePercent === null) {
    return '—'
  }

  return `${direction}${gradePercent.toLocaleString('fr-FR', {
    maximumFractionDigits: 1,
  })} %`
}

export function formatCoverage(
  track: Pick<
    Track,
    | 'pointCount'
    | 'segmentCount'
    | 'elevationPointCount'
    | 'elevationComplete'
  >,
): string {
  const points = `${track.pointCount.toLocaleString('fr-FR')} points`
  const segments = `${track.segmentCount} segment${track.segmentCount > 1 ? 's' : ''}`

  if (track.elevationPointCount === 0) {
    return `Sans altitude · ${points} · ${segments}`
  }

  if (track.elevationComplete) {
    return `Altitude complète · ${points} · ${segments}`
  }

  return `Altitude partielle ${track.elevationPointCount.toLocaleString('fr-FR')}/${track.pointCount.toLocaleString('fr-FR')} · ${segments}`
}
