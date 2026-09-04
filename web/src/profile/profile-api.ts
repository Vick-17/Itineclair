import {
  deleteJson,
  getJson,
  putJson,
} from '../api/api-client.ts'

export type ExperienceLevel =
  | 'DISCOVERING'
  | 'OCCASIONAL'
  | 'REGULAR'
  | 'EXPERIENCED'

export type HikerProfile = {
  configured: boolean
  experienceLevel: ExperienceLevel | null
  usualDurationMinutes: number | null
  usualDistanceMeters: number | null
  usualElevationGainMeters: number | null
  createdAt: string | null
  updatedAt: string | null
}

export type SaveHikerProfile = {
  experienceLevel: ExperienceLevel
  usualDurationMinutes: number | null
  usualDistanceMeters: number | null
  usualElevationGainMeters: number | null
}

export async function getHikerProfile(): Promise<HikerProfile> {
  return getJson<HikerProfile>('/profile')
}

export async function saveHikerProfile(
  profile: SaveHikerProfile,
): Promise<HikerProfile> {
  return putJson<HikerProfile>('/profile', profile)
}

export async function deleteHikerProfile(): Promise<void> {
  await deleteJson('/profile')
}
