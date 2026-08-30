import {
  getJson,
  postForm,
  putJson,
} from '../api/api-client.ts'

export type TrackFacts = {
  distanceMeters: number
  elevationGainMeters: number | null
  elevationLossMeters: number | null
  minimumElevationMeters: number | null
  maximumElevationMeters: number | null
  maximumUphillGradePercent: number | null
  maximumDownhillGradePercent: number | null
  gradeMinimumRunMeters: number
}

export type Track = {
  id: string
  name: string
  sourceFilename: string
  segmentCount: number
  pointCount: number
  elevationPointCount: number
  elevationComplete: boolean
  facts: TrackFacts | null
  createdAt: string
}

export type DaylightCondition =
  | 'NORMAL'
  | 'SUN_ALWAYS_UP'
  | 'SUN_ALWAYS_DOWN'

export type DaylightContext = {
  sunrise: string | null
  sunset: string | null
  civilDawn: string | null
  civilDusk: string | null
  expectedDaylightMinutes: number
  expectedDarknessMinutes: number
  condition: DaylightCondition
}

export type WeatherStatus =
  | 'NOT_REQUESTED'
  | 'AVAILABLE'
  | 'OUTSIDE_FORECAST_HORIZON'
  | 'UNAVAILABLE'

export type WeatherContext = {
  status: WeatherStatus
  source: string
  attributionUrl: string
  checkedAt: string | null
  validFrom: string | null
  validUntil: string | null
  minimumTemperatureCelsius: number | null
  maximumTemperatureCelsius: number | null
  minimumApparentCelsius: number | null
  maximumApparentCelsius: number | null
  maximumPrecipitationProbabilityPercent: number | null
  precipitationSumMillimeters: number | null
  snowfallSumCentimeters: number | null
  maximumWindSpeedKilometersPerHour: number | null
  maximumWindGustKilometersPerHour: number | null
  modelElevationMeters: number | null
}

export type OutdoorContext = {
  planned: true
  plannedStartLocal: string
  plannedStartAt: string
  plannedEndAt: string
  plannedDurationMinutes: number
  timeZone: string
  updatedAt: string
  daylight: DaylightContext
  weather: WeatherContext
}

type OutdoorContextResponse =
  | OutdoorContext
  | {
      planned: false
      plannedStartLocal: null
      plannedStartAt: null
      plannedEndAt: null
      plannedDurationMinutes: null
      timeZone: null
      updatedAt: null
      daylight: null
      weather: null
    }

export type SaveOutdoorContext = {
  plannedStartLocal: string
  plannedDurationMinutes: number
  timeZone: string
  shareStartPointWithWeatherProvider: boolean
}

export type AnalysisCategory =
  | 'DATA_QUALITY'
  | 'PHYSICAL_LOAD'
  | 'ROUTE_CHARACTERISTICS'
  | 'LIGHT'
  | 'WEATHER'

export type AnalysisSeverity =
  | 'NOTICE'
  | 'CAUTION'
  | 'STRONG_CAUTION'

export type EvidenceComparison =
  | 'GREATER_THAN'
  | 'GREATER_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_OR_EQUAL'

export type AnalysisEvidence = {
  metric: string
  label: string
  observedValue: number
  unit: string
  comparison: EvidenceComparison
  thresholdValue: number
}

export type AnalysisFinding = {
  code: string
  category: AnalysisCategory
  severity: AnalysisSeverity
  title: string
  explanation: string
  action: string
  evidence: AnalysisEvidence[]
}

export type ChecklistStatus = 'AVAILABLE' | 'PARTIAL' | 'TO_VERIFY'

export type AnalysisChecklistItem = {
  code: string
  status: ChecklistStatus
  title: string
  detail: string
}

export type TrackAnalysis = {
  ruleSetVersion: number
  reviewStatus: 'PROTOTYPE_AWAITING_EXPERT_REVIEW'
  generatedAt: string
  sourceSnapshot: {
    factsVersion: number | null
    outdoorContextUpdatedAt: string | null
    weatherCheckedAt: string | null
  }
  findings: AnalysisFinding[]
  checklist: AnalysisChecklistItem[]
  limitations: string[]
}

export async function listTracks(): Promise<Track[]> {
  return getJson<Track[]>('/tracks')
}

export async function getTrack(trackId: string): Promise<Track> {
  return getJson<Track>(`/tracks/${encodeURIComponent(trackId)}`)
}

export async function getTrackAnalysis(
  trackId: string,
): Promise<TrackAnalysis> {
  return getJson<TrackAnalysis>(
    `/tracks/${encodeURIComponent(trackId)}/analysis`,
  )
}

export async function importTrack(file: File): Promise<Track> {
  const formData = new FormData()
  formData.append('file', file)

  return postForm<Track>('/tracks', formData)
}

export async function getOutdoorContext(
  trackId: string,
): Promise<OutdoorContext | null> {
  const response = await getJson<OutdoorContextResponse>(
    `/tracks/${encodeURIComponent(trackId)}/outdoor-context`,
  )

  return response.planned ? response : null
}

export async function saveOutdoorContext(
  trackId: string,
  context: SaveOutdoorContext,
): Promise<OutdoorContext> {
  return putJson<OutdoorContext>(
    `/tracks/${encodeURIComponent(trackId)}/outdoor-context`,
    context,
  )
}
