import {
  useEffect,
  useId,
  useRef,
  useState,
  type DragEvent,
  type FormEvent,
} from 'react'

import { ApiError } from '../api/api-client'
import type { Account } from '../auth/auth-api'
import { PreparationFlow } from '../preparation/PreparationFlow'
import { WorkspaceAccount } from '../workspace/WorkspaceAccount'
import { WorkspaceHome } from '../workspace/WorkspaceHome'
import type { WorkspaceSection } from '../workspace/workspace-route'
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
  type TrackListItem,
} from './tracks-api'
import { trackListStatusDetails } from './track-list-status'

const MAXIMUM_FILE_SIZE_BYTES = 10 * 1024 * 1024

export function TrackDashboard({
  account,
  section,
  reportVisible,
  onNavigate,
  onReportVisibilityChange,
  onLoggedOut,
}: {
  account: Account
  section: WorkspaceSection
  reportVisible: boolean
  onNavigate: (section: WorkspaceSection) => void
  onReportVisibilityChange: (visible: boolean) => void
  onLoggedOut: () => void
}) {
  const inputId = useId()
  const inputRef = useRef<HTMLInputElement>(null)
  const activeSectionRef = useRef(section)
  const [tracks, setTracks] = useState<TrackListItem[]>([])
  const [reportTrack, setReportTrack] = useState<Track | null>(null)
  const [reportOutdoorContext, setReportOutdoorContext] =
    useState<OutdoorContext | null>(null)
  const [reportAnalysis, setReportAnalysis] =
    useState<TrackAnalysis | null>(null)
  const [reportReturnSection, setReportReturnSection] =
    useState<WorkspaceSection>('outings')
  const [openingTrackId, setOpeningTrackId] = useState<string | null>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [loadingTracks, setLoadingTracks] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  useEffect(() => {
    activeSectionRef.current = section
  }, [section])

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
      setTracks((currentTracks) => [
        {
          ...imported,
          preparationStatus: 'DEPARTURE_TO_PLAN',
        },
        ...currentTracks,
      ])
      setSelectedFile(null)
      resetInput()
      await handleOpenReport(imported.id)
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

  async function handleOpenReport(trackId: string) {
    const requestedSection = section
    setOpeningTrackId(trackId)
    setErrorMessage(null)

    try {
      const [detailedTrack, outdoorContext, analysis] = await Promise.all([
        getTrack(trackId),
        getOutdoorContext(trackId),
        getTrackAnalysis(trackId),
      ])

      if (activeSectionRef.current !== requestedSection) {
        return
      }

      setReportReturnSection(requestedSection)
      setReportTrack(detailedTrack)
      setReportOutdoorContext(outdoorContext)
      setReportAnalysis(analysis)
      onReportVisibilityChange(true)
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

  function closeReport() {
    setReportTrack(null)
    setReportOutdoorContext(null)
    setReportAnalysis(null)
    onReportVisibilityChange(false)
    onNavigate(reportReturnSection)
    void refreshTrackList()
  }

  async function refreshTrackList() {
    try {
      setTracks(await listTracks())
    } catch (error: unknown) {
      if (error instanceof ApiError && error.status === 401) {
        onLoggedOut()
        return
      }

      setErrorMessage(messageForError(error))
    }
  }

  function resetInput() {
    if (inputRef.current) {
      inputRef.current.value = ''
    }
  }

  if (reportVisible && reportTrack && reportAnalysis) {
    return (
      <PreparationFlow
        key={reportTrack.id}
        track={reportTrack}
        outdoorContext={reportOutdoorContext}
        analysis={reportAnalysis}
        onOutdoorContextChange={setReportOutdoorContext}
        onAnalysisChange={setReportAnalysis}
        onUnauthorized={onLoggedOut}
        onBack={closeReport}
      />
    )
  }

  return (
    <div className="workspace-root">
      {section !== 'account' && (errorMessage || successMessage) && (
        <div className="workspace-notices">
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
        </div>
      )}

      {section === 'home' && (
        <WorkspaceHome
          recentTrack={tracks[0] ?? null}
          loading={loadingTracks}
          openingTrackId={openingTrackId}
          onPrepare={() => onNavigate('outings')}
          onOpenReport={handleOpenReport}
          onViewOutings={() => onNavigate('outings')}
        />
      )}

      {section === 'outings' && (
        <section
          className="workspace-page workspace-outings"
          aria-labelledby="workspace-outings-title"
        >
          <header className="workspace-page-heading">
            <div>
              <p className="workspace-kicker">Bibliothèque privée</p>
              <h1 id="workspace-outings-title">Mes sorties</h1>
              <p>
                Ajoute une trace GPX ou retrouve une préparation déjà
                enregistrée.
              </p>
            </div>

            <span className="workspace-count">
              {tracks.length} trace{tracks.length > 1 ? 's' : ''}
            </span>
          </header>

          <section
            className="empty-state upload-state"
            aria-labelledby="upload-title"
          >
            <div className="empty-map" aria-hidden="true">
              <svg viewBox="0 0 240 180">
                <path d="M9 145c28-52 48-31 71-79 20-41 44 47 67 4 21-38 42 3 84-46" />
                <path d="M7 120c24-38 50-20 66-57 20-46 48 50 71 2 22-45 47 0 88-50" />
                <circle cx="10" cy="145" r="5" />
                <circle cx="231" cy="24" r="5" />
              </svg>
            </div>

            <div className="empty-copy">
              <span className="step-badge">Nouvelle préparation</span>
              <h2 id="upload-title">Ajouter une trace GPX</h2>
              <p>
                Choisis le fichier du parcours que tu veux préparer. Il reste
                privé et doit peser moins de 10 Mo.
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

                  <span className="dropzone-icon" aria-hidden="true">
                    ↥
                  </span>

                  <span>
                    <strong>
                      {selectedFile
                        ? selectedFile.name
                        : 'Choisir ou déposer un fichier'}
                    </strong>
                    <small>
                      {selectedFile
                        ? formatFileSize(selectedFile.size)
                        : 'Format .gpx · 10 Mo maximum'}
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
                  {uploading ? 'Vérification du fichier…' : 'Ajouter la trace'}
                </button>
              </form>
            </div>
          </section>

          <section className="track-library" aria-labelledby="tracks-title">
            <div className="track-library-heading">
              <div>
                <p className="auth-kicker">Toutes les traces</p>
                <h2 id="tracks-title">Préparations enregistrées</h2>
              </div>
            </div>

            {loadingTracks && (
              <p className="library-status" aria-live="polite">
                Chargement des traces…
              </p>
            )}

            {!loadingTracks && tracks.length === 0 && (
              <div className="library-status library-empty">
                <strong>Aucune trace enregistrée</strong>
                <p>La première trace ajoutée apparaîtra ici.</p>
              </div>
            )}

            {tracks.length > 0 && (
              <ul className="track-grid">
                {tracks.map((track) => {
                  const status = trackListStatusDetails(
                    track.preparationStatus,
                  )
                  const statusId = `track-${track.id}-status`

                  return (
                    <li key={track.id}>
                      <article className="track-card">
                        <div className="track-card-top">
                          <span className="track-symbol" aria-hidden="true">
                            ⌁
                          </span>
                          <time dateTime={track.createdAt}>
                            {formatDate(track.createdAt)}
                          </time>
                        </div>

                        <h3>{track.name}</h3>
                        <p className="track-filename">
                          {track.sourceFilename}
                        </p>

                        {track.facts ? (
                          <>
                            <dl>
                              <div>
                                <dt>Distance</dt>
                                <dd>
                                  {formatDistance(track.facts.distanceMeters)}
                                </dd>
                              </div>
                              <div>
                                <dt>Montée</dt>
                                <dd>
                                  {formatMeters(
                                    track.facts.elevationGainMeters,
                                  )}
                                </dd>
                              </div>
                              <div>
                                <dt>Altitudes</dt>
                                <dd>{formatElevationRange(track.facts)}</dd>
                              </div>
                              <div>
                                <dt
                                  title={`Pentes calculées sur au moins ${track.facts.gradeMinimumRunMeters} mètres`}
                                >
                                  Pentes maximales
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
                            <span>
                              Les faits seront calculés à la prochaine
                              consultation.
                            </span>
                          </div>
                        )}

                        <div
                          className={`track-list-status track-list-status-${status.tone}`}
                          id={statusId}
                        >
                          <span
                            className="track-list-status-symbol"
                            aria-hidden="true"
                          >
                            {status.symbol}
                          </span>
                          <div>
                            <strong>{status.label}</strong>
                            <p>{status.description}</p>
                          </div>
                        </div>

                        <button
                          className="track-report-button"
                          type="button"
                          onClick={() => handleOpenReport(track.id)}
                          disabled={openingTrackId !== null}
                          aria-describedby={statusId}
                          aria-label={`${status.actionLabel} : ${track.name}`}
                        >
                          {openingTrackId === track.id
                            ? 'Ouverture…'
                            : status.actionLabel}
                          <span aria-hidden="true">→</span>
                        </button>
                      </article>
                    </li>
                  )
                })}
              </ul>
            )}
          </section>
        </section>
      )}

      {section === 'account' && (
        <WorkspaceAccount account={account} onLoggedOut={onLoggedOut} />
      )}
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
  }).format(date)
}

function messageForError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return 'Le service est momentanément inaccessible. Vérifie ta connexion puis réessaie.'
}
