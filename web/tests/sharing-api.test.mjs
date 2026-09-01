import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'

import {
  buildShareUrl,
  readShareRoute,
} from '../src/sharing/share-route.ts'
import {
  createTrackShare,
  getSharedTrackReport,
  previewTrackShare,
  revokeTrackShare,
} from '../src/sharing/sharing-api.ts'

const token = 'A'.repeat(43)
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

test('share links keep the bearer token in the browser fragment', () => {
  const url = buildShareUrl(token, 'https://itineclair.example')

  assert.equal(
    url,
    `https://itineclair.example/#/share/${token}`,
  )
  assert.deepEqual(readShareRoute(`#/share/${token}`), {
    matched: true,
    token,
  })
  assert.deepEqual(readShareRoute('#/share/invalid'), {
    matched: true,
    token: null,
  })
  assert.deepEqual(readShareRoute('#/login'), {
    matched: false,
    token: null,
  })
})

test('public report sends the token only in a fixed request header', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/shared-report')
    assert.equal(url.includes(token), false)
    assert.equal(options.method, 'GET')
    assert.equal(options.credentials, 'omit')
    assert.equal(
      options.headers.get('X-Itineclair-Share-Token'),
      token,
    )
    return Response.json({ shareVersion: 1 })
  }

  const report = await getSharedTrackReport(token)
  assert.equal(report.shareVersion, 1)
})

test('preview is owner scoped and does not create a token', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/tracks/track-id/share/preview')
    assert.equal(options.credentials, 'include')
    assert.equal(options.method, 'GET')
    return Response.json({ shareVersion: 1, expiresAt: null })
  }

  const preview = await previewTrackShare('track-id')
  assert.equal(preview.expiresAt, null)
})

test('creating a share sends duration with a fresh CSRF token', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=share-csrf-token' }
  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })

    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return Response.json(
      {
        token,
        createdAt: '2026-09-01T10:00:00Z',
        expiresAt: '2026-09-08T10:00:00Z',
      },
      { status: 201 },
    )
  }

  const created = await createTrackShare('track-id', 7)

  assert.equal(created.token, token)
  assert.equal(calls[1].url, '/api/tracks/track-id/share')
  assert.equal(calls[1].options.method, 'POST')
  assert.equal(
    calls[1].options.headers.get('X-XSRF-TOKEN'),
    'share-csrf-token',
  )
  assert.deepEqual(JSON.parse(calls[1].options.body), {
    durationDays: 7,
  })
})

test('revocation uses DELETE with CSRF protection', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=revoke-csrf-token' }
  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response(null, { status: 204 })
  }

  await revokeTrackShare('track-id')

  assert.equal(calls[1].url, '/api/tracks/track-id/share')
  assert.equal(calls[1].options.method, 'DELETE')
  assert.equal(
    calls[1].options.headers.get('X-XSRF-TOKEN'),
    'revoke-csrf-token',
  )
})
