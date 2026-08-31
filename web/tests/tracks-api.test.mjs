import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'

import {
  deleteTrackFeedback,
  getOutdoorContext,
  getTrack,
  getTrackAnalysis,
  getTrackFeedback,
  importTrack,
  listTracks,
  saveOutdoorContext,
  saveTrackFeedback,
} from '../src/track/tracks-api.ts'

test('getTrackFeedback keeps a missing feedback explicit', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/tracks/track-id/feedback')
    assert.equal(options.method, 'GET')
    return Response.json({ recorded: false, trackId: 'track-id', observedIssues: [] })
  }
  assert.equal(await getTrackFeedback('track-id'), null)
})

test('saveTrackFeedback sends structured data with CSRF protection', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=feedback-token' }
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (url === '/api/auth/csrf') return new Response(null, { status: 204 })
    return Response.json({ recorded: true, outcome: 'TURNED_BACK' })
  }
  const payload = { outcome: 'TURNED_BACK', actualDurationMinutes: 180,
    perceivedEffort: 5, conditionsComparison: 'WORSE_THAN_EXPECTED',
    observedIssues: ['TERRAIN'] }
  assert.equal((await saveTrackFeedback('track-id', payload)).outcome, 'TURNED_BACK')
  assert.equal(calls[1].options.method, 'PUT')
  assert.equal(calls[1].options.headers.get('X-XSRF-TOKEN'), 'feedback-token')
  assert.deepEqual(JSON.parse(calls[1].options.body), payload)
})

test('deleteTrackFeedback uses DELETE with a fresh CSRF token', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=delete-token' }
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response(null, { status: 204 })
  }
  await deleteTrackFeedback('track-id')
  assert.equal(calls[1].options.method, 'DELETE')
  assert.equal(calls[1].options.headers.get('X-XSRF-TOKEN'), 'delete-token')
})

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

test('getTrackAnalysis requests the owner-scoped rule report', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(
      url,
      '/api/tracks/ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf/analysis',
    )
    assert.equal(options.credentials, 'include')
    assert.equal(options.method, 'GET')

    return Response.json({
      ruleSetVersion: 1,
      reviewStatus: 'PROTOTYPE_AWAITING_EXPERT_REVIEW',
      generatedAt: '2026-08-30T12:00:00Z',
      sourceSnapshot: {
        factsVersion: 1,
        outdoorContextUpdatedAt: null,
        weatherCheckedAt: null,
      },
      findings: [],
      checklist: [],
      limitations: [],
    })
  }

  const analysis = await getTrackAnalysis(
    'ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf',
  )

  assert.equal(analysis.ruleSetVersion, 1)
  assert.deepEqual(analysis.findings, [])
})

test('getOutdoorContext keeps an unplanned response explicit', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(
      url,
      '/api/tracks/ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf/outdoor-context',
    )
    assert.equal(options.credentials, 'include')
    assert.equal(options.method, 'GET')

    return Response.json({
      planned: false,
      plannedStartLocal: null,
      plannedStartAt: null,
      plannedEndAt: null,
      plannedDurationMinutes: null,
      timeZone: null,
      updatedAt: null,
      daylight: null,
      weather: null,
    })
  }

  assert.equal(
    await getOutdoorContext(
      'ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf',
    ),
    null,
  )
})

test('saveOutdoorContext sends a CSRF-protected consent choice', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=outdoor-csrf-token' }
  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })

    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return Response.json({
      planned: true,
      plannedStartLocal: '2026-08-31T08:00:00',
      plannedStartAt: '2026-08-31T06:00:00Z',
      plannedEndAt: '2026-08-31T12:00:00Z',
      plannedDurationMinutes: 360,
      timeZone: 'Europe/Paris',
      updatedAt: '2026-08-30T10:00:00Z',
      daylight: {
        sunrise: '2026-08-31T04:51:00Z',
        sunset: '2026-08-31T18:16:00Z',
        civilDawn: '2026-08-31T04:19:00Z',
        civilDusk: '2026-08-31T18:48:00Z',
        expectedDaylightMinutes: 360,
        expectedDarknessMinutes: 0,
        condition: 'NORMAL',
      },
      weather: {
        status: 'NOT_REQUESTED',
        source: 'Open-Meteo',
        attributionUrl: 'https://open-meteo.com/',
      },
    })
  }

  const saved = await saveOutdoorContext(
    'ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf',
    {
      plannedStartLocal: '2026-08-31T08:00:00',
      plannedDurationMinutes: 360,
      timeZone: 'Europe/Paris',
      shareStartPointWithWeatherProvider: false,
    },
  )

  assert.equal(saved.weather.status, 'NOT_REQUESTED')
  assert.equal(calls.length, 2)
  assert.equal(
    calls[1].url,
    '/api/tracks/ce7b58c7-d6f4-4a26-8bad-68265bdb7bbf/outdoor-context',
  )
  assert.equal(calls[1].options.method, 'PUT')
  assert.equal(
    calls[1].options.headers.get('X-XSRF-TOKEN'),
    'outdoor-csrf-token',
  )
  assert.equal(
    calls[1].options.headers.get('Content-Type'),
    'application/json',
  )
  assert.deepEqual(JSON.parse(calls[1].options.body), {
    plannedStartLocal: '2026-08-31T08:00:00',
    plannedDurationMinutes: 360,
    timeZone: 'Europe/Paris',
    shareStartPointWithWeatherProvider: false,
  })
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
