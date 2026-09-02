import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'

import {
  deleteAccount,
  exportAccountData,
} from '../src/privacy/privacy-api.ts'

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

test('account export sends the current password and returns the ZIP filename', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=csrf-token' }
  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })

    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return new Response('ZIP', {
      status: 200,
      headers: {
        'Content-Type': 'application/zip',
        'Content-Disposition':
          'attachment; filename="itineclair-export-20260901T120000Z.zip"',
      },
    })
  }

  const download = await exportAccountData(
    'une phrase de passe de test',
  )

  assert.equal(calls.length, 2)
  assert.equal(calls[1].url, '/api/account/export')
  assert.equal(calls[1].options.method, 'POST')
  assert.equal(calls[1].options.credentials, 'include')
  assert.equal(
    calls[1].options.headers.get('X-XSRF-TOKEN'),
    'csrf-token',
  )
  assert.equal(calls[1].options.headers.get('Accept'), 'application/zip')
  assert.deepEqual(JSON.parse(calls[1].options.body), {
    currentPassword: 'une phrase de passe de test',
  })
  assert.equal(
    download.fileName,
    'itineclair-export-20260901T120000Z.zip',
  )
  assert.equal(await download.blob.text(), 'ZIP')
})

test('account deletion sends both confirmations with CSRF protection', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=csrf-token' }
  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })

    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return new Response(null, { status: 204 })
  }

  await deleteAccount(
    'une phrase de passe de test',
    'victor@example.test',
  )

  assert.equal(calls.length, 2)
  assert.equal(calls[1].url, '/api/account')
  assert.equal(calls[1].options.method, 'DELETE')
  assert.equal(calls[1].options.credentials, 'include')
  assert.equal(
    calls[1].options.headers.get('X-XSRF-TOKEN'),
    'csrf-token',
  )
  assert.equal(
    calls[1].options.headers.get('Content-Type'),
    'application/json',
  )
  assert.deepEqual(JSON.parse(calls[1].options.body), {
    currentPassword: 'une phrase de passe de test',
    confirmationEmail: 'victor@example.test',
  })
})

test('unsafe download filenames are replaced by the fallback name', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=csrf-token' }

  globalThis.fetch = async (url) => {
    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return new Response('ZIP', {
      headers: {
        'Content-Disposition': 'attachment; filename="../"',
      },
    })
  }

  const download = await exportAccountData('mot de passe')

  assert.equal(download.fileName, 'itineclair-export.zip')
})
