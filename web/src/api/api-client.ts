const API_ROOT = '/api'

export type FieldViolation = {
  field: string
  message: string
}

type ProblemPayload = {
  detail?: string
  code?: string
  violations?: FieldViolation[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly violations: FieldViolation[]
  readonly retryAfterSeconds?: number

  constructor(
    message: string,
    options: {
      status: number
      code?: string
      violations?: FieldViolation[]
      retryAfterSeconds?: number
    },
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = options.status
    this.code = options.code
    this.violations = options.violations ?? []
    this.retryAfterSeconds = options.retryAfterSeconds
  }
}

export function readCookie(
  name: string,
  cookieHeader = document.cookie,
): string | null {
  const encodedName = `${encodeURIComponent(name)}=`
  const cookie = cookieHeader
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(encodedName))

  if (!cookie) {
    return null
  }

  const value = cookie.slice(encodedName.length)

  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

export async function getJson<T>(path: string): Promise<T> {
  return request<T>(path)
}

export async function postJson<T = void>(
  path: string,
  body?: unknown,
): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
    contentType: body ? 'application/json' : undefined,
    csrf: true,
  })
}

export async function putJson<T>(
  path: string,
  body: unknown,
): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    body: JSON.stringify(body),
    contentType: 'application/json',
    csrf: true,
  })
}

export async function deleteJson(path: string): Promise<void> {
  return request<void>(path, {
    method: 'DELETE',
    csrf: true,
  })
}

export async function postForm<T>(
  path: string,
  formData: FormData,
): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: formData,
    csrf: true,
  })
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: BodyInit
  contentType?: string
  csrf?: boolean
}

async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers({
    Accept: 'application/json',
  })

  if (options.contentType) {
    headers.set('Content-Type', options.contentType)
  }

  if (options.csrf) {
    headers.set('X-XSRF-TOKEN', await fetchCsrfToken())
  }

  const response = await fetch(`${API_ROOT}${path}`, {
    method: options.method ?? 'GET',
    credentials: 'include',
    headers,
    body: options.body,
  })

  if (!response.ok) {
    throw await toApiError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  try {
    return (await response.json()) as T
  } catch {
    throw new ApiError('La réponse du serveur est illisible.', {
      status: response.status,
      code: 'invalid_server_response',
    })
  }
}

async function fetchCsrfToken(): Promise<string> {
  const response = await fetch(`${API_ROOT}/auth/csrf`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw await toApiError(response)
  }

  const token = readCookie('XSRF-TOKEN')

  if (!token) {
    throw new ApiError(
      'La protection de la requête n’a pas pu être initialisée.',
      { status: 0, code: 'csrf_token_missing' },
    )
  }

  return token
}

async function toApiError(response: Response): Promise<ApiError> {
  const payload = await readProblemPayload(response)
  const retryAfterHeader = response.headers.get('Retry-After')
  const retryAfterSeconds = retryAfterHeader
    ? Number.parseInt(retryAfterHeader, 10)
    : undefined

  return new ApiError(
    payload?.detail ?? 'Une erreur inattendue est survenue.',
    {
      status: response.status,
      code: payload?.code,
      violations: payload?.violations,
      retryAfterSeconds:
        retryAfterSeconds !== undefined && Number.isFinite(retryAfterSeconds)
          ? retryAfterSeconds
          : undefined,
    },
  )
}

async function readProblemPayload(
  response: Response,
): Promise<ProblemPayload | null> {
  const contentType = response.headers.get('Content-Type') ?? ''

  if (!contentType.includes('json')) {
    return null
  }

  try {
    const payload: unknown = await response.json()

    if (!isRecord(payload)) {
      return null
    }

    return {
      detail: asString(payload.detail),
      code: asString(payload.code),
      violations: asViolations(payload.violations),
    }
  } catch {
    return null
  }
}

function asViolations(value: unknown): FieldViolation[] | undefined {
  if (!Array.isArray(value)) {
    return undefined
  }

  return value.flatMap((item) => {
    if (!isRecord(item)) {
      return []
    }

    const field = asString(item.field)
    const message = asString(item.message)

    return field && message ? [{ field, message }] : []
  })
}

function asString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
