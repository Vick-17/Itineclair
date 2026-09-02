import {
  useId,
  useState,
  type FormEvent,
} from 'react'

import { ApiError } from '../api/api-client'
import type { Account } from '../auth/auth-api'
import {
  deleteAccount,
  exportAccountData,
} from './privacy-api'

export function AccountDataPanel({
  account,
  onDeleted,
  onUnauthorized,
}: {
  account: Account
  onDeleted: () => void
  onUnauthorized: () => void
}) {
  const exportPasswordId = useId()
  const deletionPasswordId = useId()
  const confirmationEmailId = useId()
  const deletionAcknowledgementId = useId()
  const [exportPassword, setExportPassword] = useState('')
  const [deletionPassword, setDeletionPassword] = useState('')
  const [confirmationEmail, setConfirmationEmail] = useState('')
  const [deletionAcknowledged, setDeletionAcknowledged] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [exportMessage, setExportMessage] = useState<string | null>(null)
  const [deletionMessage, setDeletionMessage] = useState<string | null>(null)

  const confirmationMatches =
    confirmationEmail.trim().toLowerCase() === account.email

  async function handleExport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setExportMessage(null)
    setExporting(true)

    try {
      const download = await exportAccountData(exportPassword)
      triggerDownload(download.blob, download.fileName)
      setExportPassword('')
      setExportMessage('Ton archive privée a été préparée et téléchargée.')
    } catch (error: unknown) {
      if (isExpiredSession(error)) {
        onUnauthorized()
        return
      }

      setExportMessage(messageForError(error))
    } finally {
      setExporting(false)
    }
  }

  async function handleDeletion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setDeletionMessage(null)

    if (!confirmationMatches || !deletionAcknowledged) {
      setDeletionMessage(
        'Recopie ton adresse e-mail et confirme le caractère irréversible de la suppression.',
      )
      return
    }

    setDeleting(true)

    try {
      await deleteAccount(deletionPassword, confirmationEmail)
      onDeleted()
    } catch (error: unknown) {
      if (isExpiredSession(error)) {
        onUnauthorized()
        return
      }

      setDeletionMessage(messageForError(error))
      setDeleting(false)
    }
  }

  return (
    <section className="account-data" aria-labelledby="account-data-title">
      <div className="account-data-heading">
        <div>
          <p className="auth-kicker">Données et vie privée</p>
          <h2 id="account-data-title">Garder le contrôle de ton compte</h2>
        </div>
        <p>
          Ces actions redemandent ton mot de passe courant. Il n’est jamais
          inclus dans l’archive ni conservé en clair.
        </p>
      </div>

      <div className="account-data-grid">
        <article className="account-action-card">
          <div>
            <span className="account-action-number" aria-hidden="true">01</span>
            <h3>Exporter mes données</h3>
            <p>
              Télécharge un ZIP avec un manifeste JSON, tes informations de
              compte, tes préparations, tes retours et un GPX par trace.
            </p>
            <p className="account-data-note">
              Les GPX sont reconstruits à partir des points conservés : les
              extensions non retenues lors de l’import ne sont pas restituées.
            </p>
          </div>

          <form className="account-action-form" onSubmit={handleExport}>
            <label htmlFor={exportPasswordId}>Mot de passe courant</label>
            <input
              id={exportPasswordId}
              type="password"
              autoComplete="current-password"
              value={exportPassword}
              onChange={(event) => setExportPassword(event.target.value)}
              maxLength={128}
              required
              disabled={exporting}
            />

            {exportMessage && (
              <p className="account-action-message" role="status">
                {exportMessage}
              </p>
            )}

            <button
              className="secondary-button"
              type="submit"
              disabled={exporting || exportPassword.length === 0}
            >
              {exporting ? 'Préparation…' : 'Télécharger mon archive'}
            </button>
          </form>
        </article>

        <article className="account-action-card account-danger-card">
          <div>
            <span className="account-action-number" aria-hidden="true">02</span>
            <h3>Supprimer définitivement mon compte</h3>
            <p>
              Cette action efface le compte, toutes les traces, les points,
              les contextes météo, les retours et les partages privés.
            </p>
            <p className="account-data-note">
              Télécharge d’abord ton archive si tu souhaites conserver une
              copie. Cette suppression est irréversible.
            </p>
          </div>

          <form className="account-action-form" onSubmit={handleDeletion}>
            <label htmlFor={deletionPasswordId}>Mot de passe courant</label>
            <input
              id={deletionPasswordId}
              type="password"
              autoComplete="current-password"
              value={deletionPassword}
              onChange={(event) => setDeletionPassword(event.target.value)}
              maxLength={128}
              required
              disabled={deleting}
            />

            <label htmlFor={confirmationEmailId}>
              Recopie {account.email}
            </label>
            <input
              id={confirmationEmailId}
              type="email"
              autoComplete="off"
              value={confirmationEmail}
              onChange={(event) => setConfirmationEmail(event.target.value)}
              maxLength={254}
              required
              disabled={deleting}
            />

            <label
              className="account-danger-confirmation"
              htmlFor={deletionAcknowledgementId}
            >
              <input
                id={deletionAcknowledgementId}
                type="checkbox"
                checked={deletionAcknowledged}
                onChange={(event) =>
                  setDeletionAcknowledged(event.target.checked)
                }
                disabled={deleting}
              />
              <span>Je comprends que ces données ne pourront pas être récupérées.</span>
            </label>

            {deletionMessage && (
              <p className="account-action-message" role="alert">
                {deletionMessage}
              </p>
            )}

            <button
              className="danger-button"
              type="submit"
              disabled={
                deleting
                || deletionPassword.length === 0
                || !confirmationMatches
                || !deletionAcknowledged
              }
            >
              {deleting ? 'Suppression…' : 'Supprimer mon compte'}
            </button>
          </form>
        </article>
      </div>
    </section>
  )
}

function triggerDownload(blob: Blob, fileName: string) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  link.rel = 'noopener'
  document.body.append(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0)
}

function isExpiredSession(error: unknown): boolean {
  return error instanceof ApiError
    && error.status === 401
    && error.code !== 'invalid_current_password'
}

function messageForError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'account_action_rate_limited') {
      const seconds = error.retryAfterSeconds
      return seconds
        ? `Trop de tentatives. Réessaie dans environ ${seconds} secondes.`
        : error.message
    }

    return error.message
  }

  return 'Le service est momentanément inaccessible. Vérifie ta connexion puis réessaie.'
}
