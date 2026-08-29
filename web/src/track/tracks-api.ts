import {
  getJson,
  postForm,
} from '../api/api-client.ts'

export type Track = {
  id: string
  name: string
  sourceFilename: string
  segmentCount: number
  pointCount: number
  elevationPointCount: number
  elevationComplete: boolean
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
