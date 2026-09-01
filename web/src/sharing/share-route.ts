const SHARE_PREFIX = '#/share/'
const TOKEN_PATTERN = /^[A-Za-z0-9_-]{43}$/

export type ShareRoute =
  | { matched: false; token: null }
  | { matched: true; token: string | null }

export function readShareRoute(hash: string): ShareRoute {
  if (!hash.startsWith(SHARE_PREFIX)) {
    return { matched: false, token: null }
  }

  const token = hash.slice(SHARE_PREFIX.length)
  return {
    matched: true,
    token: TOKEN_PATTERN.test(token) ? token : null,
  }
}

export function buildShareUrl(
  token: string,
  origin = window.location.origin,
): string {
  if (!TOKEN_PATTERN.test(token)) {
    throw new Error('Invalid share token.')
  }

  return new URL(`/#/share/${token}`, origin).toString()
}
