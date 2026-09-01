import {
  deleteJson,
  getJson,
  getPublicJson,
  postJson,
} from '../api/api-client.ts'
import type {
  OutdoorContext,
  TrackAnalysis,
  TrackFacts,
} from '../track/tracks-api.ts'

const SHARE_TOKEN_HEADER = 'X-Itineclair-Share-Token'

export type TrackShareStatus = {
  active: boolean
  expiresAt: string | null
  createdAt: string | null
}

export type CreatedTrackShare = {
  token: string
  expiresAt: string
  createdAt: string
}

export type SharedTrack = {
  segmentCount: number
  pointCount: number
  elevationPointCount: number
  elevationComplete: boolean
  facts: TrackFacts | null
}

export type SharedTrackReport = {
  shareVersion: number
  expiresAt: string | null
  track: SharedTrack
  outdoorContext: OutdoorContext | null
  analysis: TrackAnalysis
  privacy: {
    excludedData: string[]
  }
}

export async function getTrackShareStatus(
  trackId: string,
): Promise<TrackShareStatus> {
  return getJson<TrackShareStatus>(sharePath(trackId))
}

export async function previewTrackShare(
  trackId: string,
): Promise<SharedTrackReport> {
  return getJson<SharedTrackReport>(`${sharePath(trackId)}/preview`)
}

export async function createTrackShare(
  trackId: string,
  durationDays: number,
): Promise<CreatedTrackShare> {
  return postJson<CreatedTrackShare>(sharePath(trackId), {
    durationDays,
  })
}

export async function revokeTrackShare(trackId: string): Promise<void> {
  return deleteJson(sharePath(trackId))
}

export async function getSharedTrackReport(
  token: string,
): Promise<SharedTrackReport> {
  return getPublicJson<SharedTrackReport>('/shared-report', {
    [SHARE_TOKEN_HEADER]: token,
  })
}

function sharePath(trackId: string): string {
  return `/tracks/${encodeURIComponent(trackId)}/share`
}
