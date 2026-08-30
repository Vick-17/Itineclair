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

export async function listTracks(): Promise<Track[]> {
  return getJson<Track[]>('/tracks')
}

export async function getTrack(trackId: string): Promise<Track> {
  return getJson<Track>(`/tracks/${encodeURIComponent(trackId)}`)
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
