import {
  getJson,
  postForm,
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

export async function listTracks(): Promise<Track[]> {
  return getJson<Track[]>('/tracks')
}

export async function importTrack(file: File): Promise<Track> {
  const formData = new FormData()
  formData.append('file', file)

  return postForm<Track>('/tracks', formData)
}
