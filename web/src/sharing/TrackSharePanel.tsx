import { useEffect, useRef, useState } from 'react'

import { ApiError } from '../api/api-client'
import { buildShareUrl } from './share-route'
import { SharedReportView } from './SharedReportView'
import {
  createTrackShare,
  getTrackShareStatus,
  previewTrackShare,
  revokeTrackShare,
  type SharedTrackReport,
  type TrackShareStatus,
} from './sharing-api'

export function TrackSharePanel({
  trackId,
  onUnauthorized,
}: {
  trackId: string
  onUnauthorized: () => void
}) {
  const linkInput = useRef<HTMLInputElement>(null)
  const [status, setStatus] = useState<TrackShareStatus | null>(null)
  const [preview, setPreview] = useState<SharedTrackReport | null>(null)
  const [durationDays, setDurationDays] = useState(7)
  const [shareUrl, setShareUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    getTrackShareStatus(trackId)
      .then((loadedStatus) => {
        if (active) {
          setStatus(loadedStatus)
        }
      })
      .catch((cause: unknown) => {
        if (!active) {
          return
        }
        handleError(cause, onUnauthorized, setError)
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [trackId, onUnauthorized])

  async function loadPreview() {
    setPending(true)
    setError(null)
    setMessage(null)

    try {
      setPreview(await previewTrackShare(trackId))
      setMessage(
        'Aperçu chargé. Vérifie-le avant de créer le lien.',
      )
    } catch (cause: unknown) {
      handleError(cause, onUnauthorized, setError)
    } finally {
      setPending(false)
    }
  }

  async function createLink() {
    if (!preview) {
      setError('Prévisualise d’abord exactement ce qui sera partagé.')
      return
    }

    setPending(true)
    setError(null)
    setMessage(null)

    try {
      const created = await createTrackShare(trackId, durationDays)
      setShareUrl(buildShareUrl(created.token))
      setStatus({
        active: true,
        createdAt: created.createdAt,
        expiresAt: created.expiresAt,
      })
      setMessage(
        'Nouveau lien créé. Tout ancien lien de cette trace est maintenant invalide.',
      )
    } catch (cause: unknown) {
      handleError(cause, onUnauthorized, setError)
    } finally {
      setPending(false)
    }
  }

  async function copyLink() {
    if (!shareUrl) {
      return
    }

    try {
      await navigator.clipboard.writeText(shareUrl)
      setMessage('Lien copié. Envoie-le uniquement à la personne choisie.')
    } catch {
      linkInput.current?.select()
      setMessage('Le lien est sélectionné : copie-le manuellement.')
    }
  }

  async function revoke() {
    if (!window.confirm('Révoquer immédiatement ce lien de partage ?')) {
      return
    }

    setPending(true)
    setError(null)
    setMessage(null)

    try {
      await revokeTrackShare(trackId)
      setStatus({ active: false, expiresAt: null, createdAt: null })
      setShareUrl(null)
      setMessage('Le lien a été révoqué immédiatement.')
    } catch (cause: unknown) {
      handleError(cause, onUnauthorized, setError)
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="track-share" aria-labelledby="track-share-title">
      <div className="track-share-heading">
        <div>
          <p className="auth-kicker">Partage privé et révocable</p>
          <h2 id="track-share-title">Partager une vue réduite du rapport</h2>
          <p>
            Le lien ne contient ni carte, ni coordonnée, ni identité, ni
            retour post-sortie. Toute personne qui le reçoit peut néanmoins
            lire le rapport jusqu’à son expiration.
          </p>
        </div>
        {!loading && status?.active && status.expiresAt && (
          <span className="share-status">
            Actif jusqu’au {formatDate(status.expiresAt)}
          </span>
        )}
      </div>

      {loading ? (
        <p className="share-loading" role="status">
          Vérification du partage…
        </p>
      ) : (
        <div className="share-controls">
          <div className="share-actions">
            <button
              className="secondary-button"
              type="button"
              onClick={loadPreview}
              disabled={pending}
            >
              {preview ? 'Actualiser l’aperçu' : 'Prévisualiser le partage'}
            </button>

            <label>
              <span>Expiration</span>
              <select
                value={durationDays}
                onChange={(event) =>
                  setDurationDays(Number(event.target.value))}
                disabled={pending}
              >
                <option value={7}>7 jours — recommandé</option>
                <option value={30}>30 jours — maximum MVP</option>
              </select>
            </label>

            <button
              className="primary-button share-create"
              type="button"
              onClick={createLink}
              disabled={pending || !preview}
            >
              {pending
                ? 'Traitement…'
                : status?.active
                  ? 'Créer un nouveau lien'
                  : 'Créer le lien privé'}
            </button>
          </div>

          {status?.active && !shareUrl && (
            <p className="share-help">
              Pour des raisons de sécurité, le lien existant n’est jamais
              relu depuis la base. Créer un nouveau lien invalidera l’ancien.
            </p>
          )}

          {shareUrl && (
            <div className="share-link" role="status">
              <label htmlFor="private-share-link">Lien affiché une seule fois</label>
              <div>
                <input
                  ref={linkInput}
                  id="private-share-link"
                  type="text"
                  value={shareUrl}
                  readOnly
                  spellCheck={false}
                />
                <button type="button" onClick={copyLink}>
                  Copier
                </button>
              </div>
            </div>
          )}

          {status?.active && (
            <button
              className="share-revoke"
              type="button"
              onClick={revoke}
              disabled={pending}
            >
              Révoquer immédiatement le lien
            </button>
          )}
        </div>
      )}

      {error && (
        <div className="form-alert" role="alert">
          <span aria-hidden="true">!</span><p>{error}</p>
        </div>
      )}
      {message && (
        <div className="success-alert" role="status">
          <span aria-hidden="true">✓</span><p>{message}</p>
        </div>
      )}

      {preview && (
        <details className="share-preview" open>
          <summary>Aperçu exact visible par le destinataire</summary>
          <div className="share-preview-frame">
            <SharedReportView report={preview} preview />
          </div>
        </details>
      )}
    </section>
  )
}

function handleError(
  cause: unknown,
  onUnauthorized: () => void,
  setError: (message: string) => void,
) {
  if (cause instanceof ApiError && cause.status === 401) {
    onUnauthorized()
    return
  }

  setError(
    cause instanceof ApiError
      ? cause.message
      : 'Le partage ne peut pas être géré pour le moment.',
  )
}

function formatDate(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return 'date inconnue'
  }
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
