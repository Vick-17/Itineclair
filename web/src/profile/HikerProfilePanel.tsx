import {
  useEffect,
  useId,
  useState,
  type FormEvent,
} from 'react'

import { ApiError } from '../api/api-client'
import {
  deleteHikerProfile,
  getHikerProfile,
  saveHikerProfile,
  type ExperienceLevel,
  type HikerProfile,
} from './profile-api'

const EXPERIENCE_OPTIONS: Array<{
  value: ExperienceLevel
  label: string
}> = [
  { value: 'DISCOVERING', label: 'Je débute ou je reprends' },
  { value: 'OCCASIONAL', label: 'Quelques sorties dans l’année' },
  { value: 'REGULAR', label: 'Plusieurs sorties par mois' },
  {
    value: 'EXPERIENCED',
    label: 'Pratique fréquente sur des terrains variés',
  },
]

export function HikerProfilePanel({
  onUnauthorized,
}: {
  onUnauthorized: () => void
}) {
  const experienceId = useId()
  const experienceHintId = useId()
  const durationId = useId()
  const durationHintId = useId()
  const distanceId = useId()
  const distanceHintId = useId()
  const elevationId = useId()
  const elevationHintId = useId()

  const [configured, setConfigured] = useState(false)
  const [experienceLevel, setExperienceLevel] =
    useState<ExperienceLevel | ''>('')
  const [usualDurationMinutes, setUsualDurationMinutes] = useState('')
  const [usualDistanceKilometers, setUsualDistanceKilometers] =
    useState('')
  const [usualElevationGainMeters, setUsualElevationGainMeters] =
    useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [hasError, setHasError] = useState(false)

  useEffect(() => {
    let active = true

    getHikerProfile()
      .then((profile) => {
        if (active) {
          applyProfile(profile)
        }
      })
      .catch((error: unknown) => {
        if (!active) {
          return
        }

        if (isUnauthorized(error)) {
          onUnauthorized()
          return
        }

        setHasError(true)
        setMessage(messageForError(error))
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [onUnauthorized])

  function applyProfile(profile: HikerProfile) {
    setConfigured(profile.configured)
    setExperienceLevel(profile.experienceLevel ?? '')
    setUsualDurationMinutes(
      toInputValue(profile.usualDurationMinutes),
    )
    setUsualDistanceKilometers(
      profile.usualDistanceMeters === null
        ? ''
        : formatKilometers(profile.usualDistanceMeters),
    )
    setUsualElevationGainMeters(
      toInputValue(profile.usualElevationGainMeters),
    )
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage(null)
    setHasError(false)

    if (!experienceLevel) {
      setHasError(true)
      setMessage(
        'Choisis le niveau qui décrit le mieux ta pratique actuelle.',
      )
      return
    }

    const duration = parseOptionalInteger(usualDurationMinutes)
    const distance = parseOptionalDistance(usualDistanceKilometers)
    const elevation =
      parseOptionalInteger(usualElevationGainMeters)

    if (
      duration === undefined
      || distance === undefined
      || elevation === undefined
    ) {
      setHasError(true)
      setMessage(
        'Vérifie les repères numériques avant d’enregistrer.',
      )
      return
    }

    setSaving(true)

    try {
      const saved = await saveHikerProfile({
        experienceLevel,
        usualDurationMinutes: duration,
        usualDistanceMeters: distance,
        usualElevationGainMeters: elevation,
      })

      applyProfile(saved)
      setMessage('Tes repères privés ont été enregistrés.')
    } catch (error: unknown) {
      if (isUnauthorized(error)) {
        onUnauthorized()
        return
      }

      setHasError(true)
      setMessage(messageForError(error))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    setMessage(null)
    setHasError(false)
    setDeleting(true)

    try {
      await deleteHikerProfile()
      applyProfile(emptyProfile())
      setMessage('Ton profil de pratique a été effacé.')
    } catch (error: unknown) {
      if (isUnauthorized(error)) {
        onUnauthorized()
        return
      }

      setHasError(true)
      setMessage(messageForError(error))
    } finally {
      setDeleting(false)
    }
  }

  const busy = loading || saving || deleting

  return (
    <section
      className="hiker-profile"
      aria-labelledby="hiker-profile-title"
    >
      <div className="hiker-profile-heading">
        <div>
          <p className="auth-kicker">
            Profil facultatif et privé
          </p>
          <h2 id="hiker-profile-title">
            Mes repères de pratique
          </h2>
        </div>

        <p>
          Ces informations sont déclaratives. Elles ne constituent
          ni une certification, ni une autorisation de partir,
          ni un avis médical, et ne modifient pas encore les
          rapports.
        </p>
      </div>

      {loading ? (
        <p className="profile-loading" aria-live="polite">
          Chargement de tes repères…
        </p>
      ) : (
        <form
          className="hiker-profile-form"
          onSubmit={handleSave}
        >
          <div className="profile-field profile-level-field">
            <label htmlFor={experienceId}>
              Pratique actuelle
            </label>

            <select
              id={experienceId}
              value={experienceLevel}
              onChange={(event) =>
                setExperienceLevel(
                  event.target.value as ExperienceLevel | '',
                )
              }
              aria-describedby={experienceHintId}
              required
              disabled={busy}
            >
              <option value="">
                Choisir une description
              </option>

              {EXPERIENCE_OPTIONS.map((option) => (
                <option
                  key={option.value}
                  value={option.value}
                >
                  {option.label}
                </option>
              ))}
            </select>

            <small id={experienceHintId}>
              Choisis selon ton expérience réelle, pas selon ton
              objectif.
            </small>
          </div>

          <div className="profile-field">
            <label htmlFor={durationId}>
              Durée habituelle (minutes)
            </label>

            <input
              id={durationId}
              type="number"
              min="15"
              max="1440"
              step="15"
              value={usualDurationMinutes}
              onChange={(event) =>
                setUsualDurationMinutes(event.target.value)
              }
              aria-describedby={durationHintId}
              placeholder="Ex. 240"
              disabled={busy}
            />

            <small id={durationHintId}>
              Facultatif · de 15 min à 24 h.
            </small>
          </div>

          <div className="profile-field">
            <label htmlFor={distanceId}>
              Distance habituelle (km)
            </label>

            <input
              id={distanceId}
              type="number"
              min="0.5"
              max="100"
              step="0.5"
              value={usualDistanceKilometers}
              onChange={(event) =>
                setUsualDistanceKilometers(event.target.value)
              }
              aria-describedby={distanceHintId}
              placeholder="Ex. 14"
              disabled={busy}
            />

            <small id={distanceHintId}>
              Facultatif · de 0,5 à 100 km.
            </small>
          </div>

          <div className="profile-field">
            <label htmlFor={elevationId}>
              Dénivelé positif habituel (m)
            </label>

            <input
              id={elevationId}
              type="number"
              min="0"
              max="10000"
              step="50"
              value={usualElevationGainMeters}
              onChange={(event) =>
                setUsualElevationGainMeters(event.target.value)
              }
              aria-describedby={elevationHintId}
              placeholder="Ex. 900"
              disabled={busy}
            />

            <small id={elevationHintId}>
              Facultatif · de 0 à 10 000 m.
            </small>
          </div>

          <div className="profile-actions">
            <button
              className="primary-button"
              type="submit"
              disabled={busy || experienceLevel === ''}
            >
              {saving
                ? 'Enregistrement…'
                : 'Enregistrer mes repères'}
            </button>

            {configured && (
              <button
                className="profile-delete-button"
                type="button"
                onClick={handleDelete}
                disabled={busy}
              >
                {deleting
                  ? 'Effacement…'
                  : 'Effacer mon profil'}
              </button>
            )}
          </div>

          {message && (
            <p
              className={
                `profile-message${
                  hasError ? ' profile-message-error' : ''
                }`
              }
              role={hasError ? 'alert' : 'status'}
            >
              {message}
            </p>
          )}
        </form>
      )}
    </section>
  )
}

function emptyProfile(): HikerProfile {
  return {
    configured: false,
    experienceLevel: null,
    usualDurationMinutes: null,
    usualDistanceMeters: null,
    usualElevationGainMeters: null,
    createdAt: null,
    updatedAt: null,
  }
}

function parseOptionalInteger(
  value: string,
): number | null | undefined {
  if (value.trim() === '') {
    return null
  }

  const parsed = Number(value)
  return Number.isInteger(parsed) ? parsed : undefined
}

function parseOptionalDistance(
  value: string,
): number | null | undefined {
  if (value.trim() === '') {
    return null
  }

  const kilometers = Number(value)

  if (!Number.isFinite(kilometers)) {
    return undefined
  }

  const meters = kilometers * 1000
  return Number.isInteger(meters) ? meters : undefined
}

function toInputValue(value: number | null): string {
  return value === null ? '' : value.toString()
}

function formatKilometers(meters: number): string {
  return (meters / 1000).toString()
}

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}

function messageForError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return 'Le profil est momentanément inaccessible. Vérifie ta connexion puis réessaie.'
}
