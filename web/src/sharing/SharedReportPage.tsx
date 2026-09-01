import { useEffect, useState } from 'react'

import { ApiError } from '../api/api-client'
import { SharedReportView } from './SharedReportView'
import {
  getSharedTrackReport,
  type SharedTrackReport,
} from './sharing-api'

type PublicReportState =
  | { status: 'loading' }
  | { status: 'ready'; report: SharedTrackReport }
  | { status: 'error'; message: string }

export function SharedReportPage({ token }: { token: string | null }) {
  const [state, setState] = useState<PublicReportState>(
    token
      ? { status: 'loading' }
      : {
          status: 'error',
          message: 'Ce lien de partage est incomplet ou invalide.',
        },
  )

  useEffect(() => {
    if (!token) {
      return
    }

    let active = true
    getSharedTrackReport(token)
      .then((report) => {
        if (active) {
          setState({ status: 'ready', report })
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setState({ status: 'error', message: publicErrorMessage(error) })
        }
      })

    return () => {
      active = false
    }
  }, [token])

  return (
    <div className="app-shell public-share-shell">
      <a className="skip-link" href="#shared-main">
        Aller au rapport partagé
      </a>
      <header className="topbar public-share-topbar">
        <a className="brand" href="/" aria-label="Itinéclair, accueil">
          <span className="brand-mark" aria-hidden="true">△</span>
          <span>Itinéclair</span>
        </a>
        <p className="topbar-tagline">Lien privé · lecture seule</p>
      </header>

      <main id="shared-main">
        {state.status === 'loading' && (
          <section className="loading-screen" aria-live="polite">
            <span className="loading-mark" aria-hidden="true" />
            <p>Ouverture du rapport partagé…</p>
          </section>
        )}

        {state.status === 'error' && (
          <section className="shared-report-error" role="alert">
            <span aria-hidden="true">!</span>
            <div>
              <h1>Partage indisponible</h1>
              <p>{state.message}</p>
              <small>
                Le lien peut avoir expiré ou avoir été révoqué par son auteur.
              </small>
            </div>
          </section>
        )}

        {state.status === 'ready' && (
          <SharedReportView report={state.report} />
        )}
      </main>

      <footer className="site-footer">
        <p>Itinéclair · Aide prudente à la préparation montagne</p>
        <p>Les conditions réelles peuvent changer rapidement.</p>
      </footer>
    </div>
  )
}

function publicErrorMessage(error: unknown): string {
  if (
    error instanceof ApiError
    && error.code === 'shared_report_not_found'
  ) {
    return 'Ce partage n’existe plus ou n’est pas accessible.'
  }

  return 'Le rapport partagé ne peut pas être chargé pour le moment.'
}
