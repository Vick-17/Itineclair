import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'

import {
  ApiError,
  readCookie,
} from '../src/api/api-client.ts'
import {
  currentAccount,
  login,
} from '../src/auth/auth-api.ts'

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

test('readCookie returns the decoded cookie without matching a prefix', () => {
  const cookies = 'OTHER_XSRF-TOKEN=wrong; XSRF-TOKEN=token%2Bvalue'

  assert.equal(readCookie('XSRF-TOKEN', cookies), 'token+value')
  assert.equal(readCookie('missing', cookies), null)
})

test('currentAccount treats an unauthenticated response as a guest', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/auth/me')
    assert.equal(options.credentials, 'include')
    return new Response(null, { status: 401 })
  }

  assert.equal(await currentAccount(), null)
})

test('login refreshes CSRF and sends the cookie token in the header', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=csrf-token' }
  const calls = []

  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })

    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return Response.json({
      id: '936dd470-a45c-46fa-a0bd-94a76e4b836a',
      email: 'victor@example.test',
      role: 'USER',
    })
  }

  const account = await login(
    'victor@example.test',
    'une phrase de passe de test',
  )

  assert.equal(account.email, 'victor@example.test')
  assert.equal(calls.length, 2)
  assert.equal(calls[1].url, '/api/auth/login')
  assert.equal(calls[1].options.method, 'POST')
  assert.equal(calls[1].options.credentials, 'include')
  assert.equal(calls[1].options.headers.get('X-XSRF-TOKEN'), 'csrf-token')
  assert.deepEqual(JSON.parse(calls[1].options.body), {
    email: 'victor@example.test',
    password: 'une phrase de passe de test',
  })
})

test('login preserves a structured rate-limit error', async () => {
  globalThis.document = { cookie: 'XSRF-TOKEN=csrf-token' }

  globalThis.fetch = async (url) => {
    if (url === '/api/auth/csrf') {
      return new Response(null, { status: 204 })
    }

    return Response.json(
      {
        title: 'Connexion temporairement limitée',
        detail: 'Trop de tentatives de connexion. Réessaie plus tard.',
        code: 'login_rate_limited',
      },
      {
        status: 429,
        headers: { 'Retry-After': '42' },
      },
    )
  }

  await assert.rejects(
    () => login('victor@example.test', 'mauvais mot de passe'),
    (error) => {
      assert.ok(error instanceof ApiError)
      assert.equal(error.status, 429)
      assert.equal(error.code, 'login_rate_limited')
      assert.equal(error.retryAfterSeconds, 42)
      assert.doesNotMatch(error.message, /victor@example\.test/)
      return true
    },
  )
})
