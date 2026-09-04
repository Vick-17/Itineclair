import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'

import {
  deleteHikerProfile,
  getHikerProfile,
  saveHikerProfile,
} from '../src/profile/profile-api.ts'

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

test('getHikerProfile requests only the authenticated profile', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/profile')
    assert.equal(options.method, 'GET')
    assert.equal(options.credentials, 'include')

    return Response.json({
      configured: false,
      experienceLevel: null,
      usualDurationMinutes: null,
      usualDistanceMeters: null,
      usualElevationGainMeters: null,
      createdAt: null,
      updatedAt: null,
    })
  }

  assert.equal((await getHikerProfile()).configured, false)
})

test('saveHikerProfile sends meters with a fresh CSRF token', async () => {
  globalThis.document = {
    cookie: 'XSRF-TOKEN=profile-token',
  }

  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })

    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return Response.json({
      configured: true,
      experienceLevel: 'REGULAR',
      usualDurationMinutes: 360,
      usualDistanceMeters: 14000,
      usualElevationGainMeters: 900,
      createdAt: '2026-09-02T10:00:00Z',
      updatedAt: '2026-09-02T10:00:00Z',
    })
  }

  const payload = {
    experienceLevel: 'REGULAR',
    usualDurationMinutes: 360,
    usualDistanceMeters: 14000,
    usualElevationGainMeters: 900,
  }

  const saved = await saveHikerProfile(payload)

  assert.equal(saved.experienceLevel, 'REGULAR')
  assert.equal(calls.length, 2)
  assert.equal(calls[1].url, '/api/profile')
  assert.equal(calls[1].options.method, 'PUT')
  assert.equal(calls[1].options.credentials, 'include')
  assert.equal(
    calls[1].options.headers.get('X-XSRF-TOKEN'),
    'profile-token',
  )
  assert.deepEqual(
    JSON.parse(calls[1].options.body),
    payload,
  )
})

test('deleteHikerProfile uses DELETE with CSRF protection', async () => {
  globalThis.document = {
    cookie: 'XSRF-TOKEN=delete-token',
  }

  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response(null, { status: 204 })
  }

  await deleteHikerProfile()

  assert.equal(calls.length, 2)
  assert.equal(calls[1].url, '/api/profile')
  assert.equal(calls[1].options.method, 'DELETE')
  assert.equal(
    calls[1].options.headers.get('X-XSRF-TOKEN'),
    'delete-token',
  )
})
