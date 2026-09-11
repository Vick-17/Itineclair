export type WorkspaceSection = 'home' | 'outings' | 'account'

const HASH_BY_SECTION: Record<WorkspaceSection, string> = {
  home: '#/home',
  outings: '#/outings',
  account: '#/account',
}

export function readWorkspaceSection(hash: string): WorkspaceSection {
  const entry = Object.entries(HASH_BY_SECTION).find(
    ([, sectionHash]) => sectionHash === hash,
  )

  return (entry?.[0] as WorkspaceSection | undefined) ?? 'home'
}

export function workspaceHash(section: WorkspaceSection): string {
  return HASH_BY_SECTION[section]
}
