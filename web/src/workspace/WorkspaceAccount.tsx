import { useState } from 'react'

import { ApiError } from '../api/api-client'
import { logout, type Account } from '../auth/auth-api'
import { AccountDataPanel } from '../privacy/AccountDataPanel'
import { HikerProfilePanel } from '../profile/HikerProfilePanel'

export function WorkspaceAccount({
  account,
  onLoggedOut,
}: {
  account: Account
  onLoggedOut: () => void
}) {
  const [loggingOut, setLoggingOut] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleLogout() {
    setLoggingOut(true)
    setErrorMessage(null)

    try {
      await logout()
      onLoggedOut()
    } catch (error: unknown) {
      setErrorMessage(messageForError(error))
      setLoggingOut(false)
    }
  }

  return (
    <section
      className="workspace-page workspace-account"
      aria-labelledby="workspace-account-title"
    >
      <header className="workspace-page-heading">
        <div>
          <p className="workspace-kicker">Réglages</p>
          <h1 id="workspace-account-title">Compte</h1>
          <p>
            Tes repères et tes données restent ici, à l’écart de la
            préparation quotidienne.
          </p>
        </div>

        <button
          className="secondary-button"
          type="button"
          onClick={handleLogout}
          disabled={loggingOut}
        >
          {loggingOut ? 'Déconnexion…' : 'Se déconnecter'}
        </button>
      </header>

      <dl className="workspace-account-summary">
        <div>
          <dt>Adresse e-mail</dt>
          <dd>{account.email}</dd>
        </div>
        <div>
          <dt>Visibilité</dt>
          <dd>Compte et préparations privés</dd>
        </div>
      </dl>

      {errorMessage && (
        <div className="form-alert dashboard-alert" role="alert">
          <span aria-hidden="true">!</span>
          <p>{errorMessage}</p>
        </div>
      )}

      <HikerProfilePanel onUnauthorized={onLoggedOut} />

      <AccountDataPanel
        account={account}
        onDeleted={onLoggedOut}
        onUnauthorized={onLoggedOut}
      />
    </section>
  )
}

function messageForError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return 'La déconnexion a échoué. Vérifie ta connexion puis réessaie.'
}
