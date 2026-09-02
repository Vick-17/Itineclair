import {
  useEffect,
  useId,
  useRef,
  useState,
  type DragEvent,
  type FormEvent,
} from 'react'

import { ApiError } from '../api/api-client'
import { logout, type Account } from '../auth/auth-api'
import { AccountDataPanel } from '../privacy/AccountDataPanel'
import { TrackReport } from './TrackReport'
import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatMaximumGrades,
  formatMeters,
} from './track-format'
import {
  getOutdoorContext,
  getTrack,
  getTrackAnalysis,
  importTrack,
  listTracks,
  type OutdoorContext,
  type Track,
  type TrackAnalysis,
} from './tracks-api'

const MAXIMUM_FILE_SIZE_BYTES = 10 * 1024 * 1024

export function TrackDashboard({
  account,
  onLoggedOut,
}: {
  account: Account
  onLoggedOut: () => void
}) {
  const inputId = useId()
  const inputRef = useRef<HTMLInputElement>(null)
  const [tracks, setTracks] = useState<Track[]>([])
  const [reportTrack, setReportTrack] = useState<Track | null>(null)
  const [reportOutdoorContext, setReportOutdoorContext] =
    useState<OutdoorContext | null>(null)
  const [reportAnalysis, setReportAnalysis] =
    useState<TrackAnalysis | null>(null)
  const [openingTrackId, setOpeningTrackId] = useState<string | null>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [loadingTracks, setLoadingTracks] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    listTracks()
      .then((loadedTracks) => {
        if (active) {
          setTracks(loadedTracks)
        }
      })
      .catch((error: unknown) => {
        if (!active) {
          return
        }

        if (error instanceof ApiError && error.status === 401) {
          onLoggedOut()
          return
        }

        setErrorMessage(messageForError(error))
      })
      .finally(() => {
        if (active) {
          setLoadingTracks(false)
        }
      })

    return () => {
      active = false
    }
  }, [onLoggedOut])

  function chooseFile(file: File | null) {
    setErrorMessage(null)
    setSuccessMessage(null)

    if (!file) {
      setSelectedFile(null)
      return
    }

    if (!file.name.toLowerCase().endsWith('.gpx')) {
      setSelectedFile(null)
      setErrorMessage('Sélectionne un fichier avec l’extension .gpx.')
      resetInput()
      return
    }

    if (file.size === 0) {
      setSelectedFile(null)
      setErrorMessage('Le fichier GPX sélectionné est vide.')
      resetInput()
      return
    }

    if (file.size > MAXIMUM_FILE_SIZE_BYTES) {
      setSelectedFile(null)
      setErrorMessage('Le fichier GPX ne doit pas dépasser 10 Mo.')
      resetInput()
      return
    }

    setSelectedFile(file)
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    chooseFile(event.dataTransfer.files.item(0))
  }

  async function handleImport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage(null)
    setSuccessMessage(null)

    if (!selectedFile) {
      setErrorMessage('Sélectionne un fichier GPX avant de lancer l’import.')
      return
    }

    setUploading(true)

    try {
      const imported = await importTrack(selectedFile)
      setTracks((currentTracks) => [imported, ...currentTracks])
      setSuccessMessage(
        `« ${imported.name} » a été importée avec ${imported.pointCount.toLocaleString('fr-FR')} points.`,
      )
      setSelectedFile(null)
      resetInput()
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        onLoggedOut()
        return
      }

      setErrorMessage(messageForError(error))
    } finally {
      setUploading(false)
    }
  }

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

  async function handleOpenReport(trackId: string) {
    setOpeningTrackId(trackId)
    setErrorMessage(null)

    try {
      const [detailedTrack, outdoorContext, analysis] = await Promise.all([
        getTrack(trackId),
        getOutdoorContext(trackId),
        getTrackAnalysis(trackId),
      ])
      setReportTrack(detailedTrack)
      setReportOutdoorContext(outdoorContext)
      setReportAnalysis(analysis)
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        onLoggedOut()
        return
      }

      setErrorMessage(messageForError(error))
    } finally {
      setOpeningTrackId(null)
    }
  }

  function resetInput() {
    if (inputRef.current) {
      inputRef.current.value = ''
    }
  }

  if (reportTrack && reportAnalysis) {
    return (
      <TrackReport
        track={reportTrack}
        outdoorContext={reportOutdoorContext}
        analysis={reportAnalysis}
        onOutdoorContextChange={setReportOutdoorContext}
        onAnalysisChange={setReportAnalysis}
        onUnauthorized={onLoggedOut}
        onBack={() => {
          setReportTrack(null)
          setReportOutdoorContext(null)
          setReportAnalysis(null)
        }}
      />
    )
  }

  return (
    <div className="dashboard">
      <section className="dashboard-heading">
        <div>
          <p className="eyebrow">
            <span aria-hidden="true">●</span>
            Espace personnel
          </p>
          <h1>Prépare ta prochaine sortie.</h1>
          <p>
            Importe une trace GPX privée. Itinéclair vérifie sa structure et
            conserve ses segments avant d’en calculer les faits utiles.
          </p>
        </div>

        <button
          className="secondary-button"
          type="button"
          onClick={handleLogout}
          disabled={loggingOut || uploading}
        >
          {loggingOut ? 'Déconnexion…' : 'Se déconnecter'}
        </button>
      </section>

      {errorMessage && (
        <div className="form-alert dashboard-alert" role="alert">
          <span aria-hidden="true">!</span>
          <p>{errorMessage}</p>
        </div>
      )}

      {successMessage && (
        <div className="success-alert dashboard-alert" role="status">
          <span aria-hidden="true">✓</span>
          <p>{successMessage}</p>
        </div>
      )}

      <section className="empty-state upload-state" aria-labelledby="upload-title">
        <div className="empty-map" aria-hidden="true">
          <svg viewBox="0 0 240 180">
            <path d="M9 145c28-52 48-31 71-79 20-41 44 47 67 4 21-38 42 3 84-46" />
            <path d="M7 120c24-38 50-20 66-57 20-46 48 50 71 2 22-45 47 0 88-50" />
            <circle cx="10" cy="145" r="5" />
            <circle cx="231" cy="24" r="5" />
          </svg>
        </div>

        <div className="empty-copy">
          <span className="step-badge">Import sécurisé</span>
          <h2 id="upload-title">Ajouter une trace GPX</h2>
          <p>
            GPX 1.0 ou 1.1, 10 Mo maximum et 50 000 points. Les fichiers XML
            externes et les entités DTD sont refusés.
          </p>

          <form className="gpx-form" onSubmit={handleImport}>
            <label
              className="gpx-dropzone"
              htmlFor={inputId}
              onDragOver={(event) => event.preventDefault()}
              onDrop={handleDrop}
            >
              <input
                ref={inputRef}
                className="visually-hidden"
                id={inputId}
                type="file"
                accept=".gpx,application/gpx+xml,application/xml,text/xml"
                onChange={(event) =>
                  chooseFile(event.currentTarget.files?.item(0) ?? null)
                }
                disabled={uploading}
              />
              <span className="dropzone-icon" aria-hidden="true">↥</span>
              <span>
                <strong>
                  {selectedFile
                    ? selectedFile.name
                    : 'Choisir ou déposer un fichier'}
                </strong>
                <small>
                  {selectedFile
                    ? formatFileSize(selectedFile.size)
                    : 'Le fichier original ne sera pas exposé publiquement.'}
                </small>
              </span>
            </label>

            <button
              className="primary-button"
              type="submit"
              disabled={uploading || !selectedFile}
            >
              {uploading && (
                <span className="button-spinner" aria-hidden="true" />
              )}
              {uploading ? 'Analyse du fichier…' : 'Importer la trace'}
            </button>
          </form>
        </div>
      </section>

      <section className="track-library" aria-labelledby="tracks-title">
        <div className="track-library-heading">
          <div>
            <p className="auth-kicker">Bibliothèque privée</p>
            <h2 id="tracks-title">Mes traces</h2>
          </div>
          <span>
            {tracks.length} trace{tracks.length > 1 ? 's' : ''}
          </span>
        </div>

        {loadingTracks && (
          <p className="library-status" aria-live="polite">
            Chargement des traces…
          </p>
        )}

        {!loadingTracks && tracks.length === 0 && (
          <div className="library-status library-empty">
            <strong>Aucune trace enregistrée</strong>
            <p>Ton premier import apparaîtra ici et restera lié à ton compte.</p>
          </div>
        )}

        {tracks.length > 0 && (
          <ul className="track-grid">
            {tracks.map((track) => (
              <li key={track.id}>
                <article className="track-card">
                  <div className="track-card-top">
                    <span className="track-symbol" aria-hidden="true">⌁</span>
                    <time dateTime={track.createdAt}>
                      {formatDate(track.createdAt)}
                    </time>
                  </div>
                  <h3>{track.name}</h3>
                  <p className="track-filename">{track.sourceFilename}</p>
                  {track.facts ? (
                    <>
                      <dl>
                        <div>
                          <dt>Distance</dt>
                          <dd>{formatDistance(track.facts.distanceMeters)}</dd>
                        </div>
                        <div>
                          <dt>D+ GPX</dt>
                          <dd>{formatMeters(track.facts.elevationGainMeters)}</dd>
                        </div>
                        <div>
                          <dt>Altitudes</dt>
                          <dd>{formatElevationRange(track.facts)}</dd>
                        </div>
                        <div>
                          <dt
                            title={`Pentes calculées sur au moins ${track.facts.gradeMinimumRunMeters} mètres`}
                          >
                            Pentes max
                          </dt>
                          <dd>{formatMaximumGrades(track.facts)}</dd>
                        </div>
                      </dl>
                      <p className="track-coverage">
                        {formatCoverage(track)}
                      </p>
                    </>
                  ) : (
                    <div className="track-facts-unavailable">
                      <strong>Calcul en attente</strong>
                      <span>Les points seront analysés à la prochaine consultation.</span>
                    </div>
                  )}
                  <button
                    className="track-report-button"
                    type="button"
                    onClick={() => handleOpenReport(track.id)}
                    disabled={openingTrackId !== null}
                    aria-label={`Ouvrir le rapport de ${track.name}`}
                  >
                    {openingTrackId === track.id
                      ? 'Ouverture…'
                      : 'Rapport et retour terrain'}
                    <span aria-hidden="true">→</span>
                  </button>
                </article>
              </li>
            ))}
          </ul>
        )}
      </section>

      <AccountDataPanel
        account={account}
        onDeleted={onLoggedOut}
        onUnauthorized={onLoggedOut}
      />

      <section className="principles" aria-label="Garanties de l’import">
        <article>
          <span>01</span>
          <h2>Fichier vérifié</h2>
          <p>Le parseur bloque les constructions XML externes et les coordonnées invalides.</p>
        </article>
        <article>
          <span>02</span>
          <h2>Segments préservés</h2>
          <p>Les ruptures de trace restent distinctes pour éviter de fausser les futurs calculs.</p>
        </article>
        <article>
          <span>03</span>
          <h2>Propriété isolée</h2>
          <p>Chaque trace est rattachée au compte authentifié et listée uniquement pour lui.</p>
        </article>
      </section>
    </div>
  )
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} Ko`
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
}

function formatDate(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return 'Date inconnue'
  }

  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function messageForError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return 'Le service est momentanément inaccessible. Vérifie ta connexion puis réessaie.'
}
