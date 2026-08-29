import {
  ApiError,
  getJson,
  postJson,
} from '../api/api-client'

export type Account = {
  id: string
  email: string
  role: 'USER'
}

export async function currentAccount(): Promise<Account | null> {
  try {
    return await getJson<Account>('/auth/me')
  } catch (error: unknown) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

export async function register(
  email: string,
  password: string,
): Promise<void> {
  await postJson('/auth/register', { email, password })
}

export async function login(
  email: string,
  password: string,
): Promise<Account> {
  return postJson<Account>('/auth/login', { email, password })
}

export async function logout(): Promise<void> {
  await postJson('/auth/logout')
}
