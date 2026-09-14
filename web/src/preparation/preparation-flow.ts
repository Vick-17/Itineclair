import type { OutdoorContext } from '../track/tracks-api.ts'

export type PreparationStep = 'route' | 'departure' | 'report'

const STEP_DETAILS: Record<
  PreparationStep,
  { number: string; label: string }
> = {
  route: { number: '1', label: 'Trace' },
  departure: { number: '2', label: 'Départ' },
  report: { number: '3', label: 'Préparation' },
}

export function preparationStepDetails(step: PreparationStep) {
  return STEP_DETAILS[step]
}

export function initialPreparationStep(
  outdoorContext: OutdoorContext | null,
): PreparationStep {
  return outdoorContext ? 'report' : 'route'
}

export function defaultPlannedStart(now = new Date()): string {
  const date = new Date(now)
  date.setDate(date.getDate() + 1)
  date.setHours(8, 0, 0, 0)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}T08:00`
}

export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Paris'
}

export function normalizeLocalDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}
