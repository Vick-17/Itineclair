import {
  workspaceHash,
  type WorkspaceSection,
} from './workspace-route'

const ITEMS: ReadonlyArray<{
  section: WorkspaceSection
  label: string
}> = [
  { section: 'home', label: 'Accueil' },
  { section: 'outings', label: 'Mes sorties' },
  { section: 'account', label: 'Compte' },
]

export function WorkspaceNavigation({
  activeSection,
  onNavigate,
  className = '',
  label = 'Navigation principale',
}: {
  activeSection: WorkspaceSection
  onNavigate?: (section: WorkspaceSection) => void
  className?: string
  label?: string
}) {
  return (
    <nav
      className={`workspace-navigation ${className}`.trim()}
      aria-label={label}
    >
      {ITEMS.map((item) => (
        <a
          key={item.section}
          href={workspaceHash(item.section)}
          onClick={() => onNavigate?.(item.section)}
          aria-current={
            activeSection === item.section ? 'page' : undefined
          }
        >
          {item.label}
        </a>
      ))}
    </nav>
  )
}
