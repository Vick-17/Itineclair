import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'

import {
  getTrack,
  importTrack,
  listTracks,
} from '../src/track/tracks-api.ts'

const originalFetch = globalThis.fetch
const originalDocument = globalThis.document

afterEach(() => {
  globalThis.fetch = originalFetch

  if (originalDocument === undefined) {
    delete globalThis.document
  } else {
    globalThis.document = originalDocument
  }
})

test('listTracks requests only the authenticated account collection', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/tracks')
    assert.equal(options.credentials, 'include')
    assert.equal(options.method, 'GET')
    return Response.json([])
  }

  assert.deepEqual(await listTracks(), [])
})

test('getTrack requests a private report by identifier', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(
      url,
      '/api/tracks/ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf',
    )
    assert.equal(options.credentials, 'include')
    assert.equal(options.method, 'GET')

    return Response.json({
      id: 'ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf',
      name: 'Tour du lac',
      facts: null,
    })
  }

  const track = await getTrack(
    'ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf',
  )

  assert.equal(track.name, 'Tour du lac')
})

test('importTrack sends GPX as multipart with a fresh CSRF token', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=track-csrf-token' }
  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })

    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return Response.json(
      {
        id: 'ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf',
        name: 'Tour du lac',
        sourceFilename: 'tour-du-lac.gpx',
        segmentCount: 1,
        pointCount: 2,
        elevationPointCount: 2,
        elevationComplete: true,
        facts: {
          distanceMeters: 12_450.5,
          elevationGainMeters: 850,
          elevationLossMeters: 810,
          minimumElevationMeters: 1_020,
          maximumElevationMeters: 1_870,
          maximumUphillGradePercent: 18.4,
          maximumDownhillGradePercent: 21.2,
          gradeMinimumRunMeters: 25,
        },
        createdAt: '2026-08-29T12:00:00Z',
      },
      { status: 201 },
    )
  }

  const file = new File(
    ['<gpx/>'],
    'tour-du-lac.gpx',
    { type: 'application/gpx+xml' },
  )

  const imported = await importTrack(file)

  assert.equal(imported.name, 'Tour du lac')
  assert.equal(imported.facts.distanceMeters, 12_450.5)
  assert.equal(imported.facts.gradeMinimumRunMeters, 25)
  assert.equal(calls.length, 2)
  assert.equal(calls[1].url, '/api/tracks')
  assert.equal(calls[1].options.method, 'POST')
  assert.equal(calls[1].options.headers.get('X-XSRF-TOKEN'), 'track-csrf-token')
  assert.equal(calls[1].options.headers.has('Content-Type'), false)
  assert.ok(calls[1].options.body instanceof FormData)
  assert.equal(calls[1].options.body.get('file').name, 'tour-du-lac.gpx')
})
