import { useEffect, useId, useState, type FormEvent } from 'react'

import { ApiError } from '../api/api-client'
import {
  deleteTrackFeedback,
  getTrackFeedback,
  saveTrackFeedback,
  type ConditionsComparison,
  type FeedbackIssue,
  type FeedbackOutcome,
  type TrackFeedback,
} from './tracks-api'

const outcomes: { value: FeedbackOutcome; label: string }[] = [
  { value: 'COMPLETED_AS_PLANNED', label: 'Terminée comme prévu' },
  { value: 'COMPLETED_WITH_CHANGES', label: 'Terminée avec adaptations' },
  { value: 'TURNED_BACK', label: 'Demi-tour ou interruption' },
  { value: 'NOT_STARTED', label: 'Non démarrée' },
]
const conditions: { value: ConditionsComparison; label: string }[] = [
  { value: 'BETTER_THAN_EXPECTED', label: 'Meilleures que prévu' },
  { value: 'AS_EXPECTED', label: 'Comme prévu' },
  { value: 'WORSE_THAN_EXPECTED', label: 'Pires que prévu' },
  { value: 'NOT_COMPARED', label: 'Non comparées' },
]
const issueOptions: { value: FeedbackIssue; label: string }[] = [
  { value: 'WEATHER', label: 'Météo' },
  { value: 'TERRAIN', label: 'Terrain' },
  { value: 'FATIGUE', label: 'Fatigue' },
  { value: 'NAVIGATION', label: 'Orientation' },
  { value: 'EQUIPMENT', label: 'Équipement' },
]
const effortOptions: { value: number | null; label: string }[] = [
  { value: null, label: 'Non renseigné' },
  ...[1, 2, 3, 4, 5].map((value) => ({ value, label: String(value) })),
]

export function TrackFeedbackPanel({ trackId, onUnauthorized }: {
  trackId: string
  onUnauthorized: () => void
}) {
  const id = useId()
  const [feedback, setFeedback] = useState<TrackFeedback | null>(null)
  const [outcome, setOutcome] = useState<FeedbackOutcome | ''>('')
  const [duration, setDuration] = useState('')
  const [effort, setEffort] = useState<number | null>(null)
  const [comparison, setComparison] = useState<ConditionsComparison>('NOT_COMPARED')
  const [issues, setIssues] = useState<FeedbackIssue[]>([])
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  function fill(value: TrackFeedback) {
    setOutcome(value.outcome)
    setDuration(value.actualDurationMinutes === null ? '' : String(value.actualDurationMinutes))
    setEffort(value.perceivedEffort)
    setComparison(value.conditionsComparison)
    setIssues(value.observedIssues)
  }

  useEffect(() => {
    let active = true
    getTrackFeedback(trackId).then((value) => {
      if (!active) return
      setFeedback(value)
      if (value) fill(value)
    }).catch((cause: unknown) => {
      if (!active) return
      if (cause instanceof ApiError && cause.status === 401) onUnauthorized()
      else setError(errorMessage(cause))
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [trackId, onUnauthorized])

  function chooseOutcome(value: FeedbackOutcome) {
    setOutcome(value)
    setMessage(null)
    setError(null)
    if (value === 'NOT_STARTED') {
      setDuration('')
      setEffort(null)
      setComparison('NOT_COMPARED')
      setIssues([])
    }
  }

  function clearStatus() {
    setMessage(null)
    setError(null)
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!outcome) return setError('Indique d’abord comment la sortie s’est terminée.')
    setPending(true); setError(null); setMessage(null)
    const notStarted = outcome === 'NOT_STARTED'
    try {
      const value = await saveTrackFeedback(trackId, {
        outcome,
        actualDurationMinutes: notStarted || !duration ? null : Number(duration),
        perceivedEffort: notStarted ? null : effort,
        conditionsComparison: notStarted ? 'NOT_COMPARED' : comparison,
        observedIssues: notStarted ? [] : issues,
      })
      setFeedback(value); fill(value); setMessage('Retour terrain enregistré.')
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) onUnauthorized()
      else setError(errorMessage(cause))
    } finally { setPending(false) }
  }

  async function remove() {
    if (!window.confirm('Supprimer définitivement ce retour terrain ?')) return
    setPending(true); setError(null); setMessage(null)
    try {
      await deleteTrackFeedback(trackId)
      setFeedback(null); setOutcome(''); setDuration(''); setEffort(null)
      setComparison('NOT_COMPARED'); setIssues([]); setMessage('Le retour terrain a été supprimé.')
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) onUnauthorized()
      else setError(errorMessage(cause))
    } finally { setPending(false) }
  }

  const detailsDisabled = pending || outcome === 'NOT_STARTED'
  return <section className="track-feedback" aria-labelledby={`${id}-title`}>
    <div className="feedback-heading"><div>
      <p className="auth-kicker">Après la sortie · moins de 30 secondes</p>
      <h2 id={`${id}-title`}>Comparer le plan au terrain</h2>
      <p>Ce retour reste privé et structuré. Il ne modifie pas le rapport de sécurité et ne contient aucun commentaire libre.</p>
    </div>{feedback && <time dateTime={feedback.updatedAt}>Retour déjà enregistré</time>}</div>
    {loading ? <div className="feedback-loading" role="status">Chargement du retour terrain…</div> :
      <form className="feedback-form" onSubmit={save}>
        <fieldset className="feedback-fieldset" disabled={pending}><legend>Comment la sortie s’est-elle terminée ?</legend>
          <div className="feedback-outcomes">{outcomes.map((option) => <label className="feedback-choice" key={option.value}>
            <input type="radio" name={`${id}-outcome`} checked={outcome === option.value} onChange={() => chooseOutcome(option.value)} required />
            <strong>{option.label}</strong></label>)}</div>
        </fieldset>
        <div className="feedback-details" aria-disabled={detailsDisabled}>
          <label className="feedback-duration"><span>Durée réelle en minutes <small>(facultatif)</small></span>
            <input type="number" min={1} max={1440} step={1} value={duration} onChange={(event) => { clearStatus(); setDuration(event.target.value) }} disabled={detailsDisabled} /></label>
          <fieldset className="feedback-fieldset" disabled={detailsDisabled}><legend>Effort ressenti <small>(facultatif)</small></legend>
            <div className="feedback-scale">{effortOptions.map((option) => <label key={option.label}><input type="radio" name={`${id}-effort`} checked={effort === option.value} onChange={() => { clearStatus(); setEffort(option.value) }} /><span>{option.label}</span></label>)}</div>
          </fieldset>
          <fieldset className="feedback-fieldset" disabled={detailsDisabled}><legend>Conditions rencontrées</legend><div className="feedback-inline-options">
            {conditions.map((option) => <label key={option.value}><input type="radio" name={`${id}-conditions`} checked={comparison === option.value} onChange={() => { clearStatus(); setComparison(option.value) }} /><span>{option.label}</span></label>)}</div></fieldset>
          <fieldset className="feedback-fieldset" disabled={detailsDisabled}><legend>Difficultés observées <small>(facultatif)</small></legend><div className="feedback-inline-options">
            {issueOptions.map((option) => <label key={option.value}><input type="checkbox" checked={issues.includes(option.value)} onChange={(event) => { clearStatus(); setIssues((old) => event.target.checked ? [...old, option.value] : old.filter((item) => item !== option.value)) }} /><span>{option.label}</span></label>)}</div></fieldset>
        </div>
        {error && <div className="form-alert" role="alert"><span>!</span><p>{error}</p></div>}
        {message && <div className="success-alert" role="status"><span>✓</span><p>{message}</p></div>}
        <div className="feedback-actions">{feedback && <button className="feedback-delete" type="button" onClick={remove} disabled={pending}>Supprimer mon retour</button>}
          <button className="primary-button feedback-save" type="submit" disabled={pending}>{feedback ? 'Mettre à jour mon retour' : 'Enregistrer mon retour'}</button></div>
      </form>}
  </section>
}

function errorMessage(cause: unknown) {
  return cause instanceof ApiError ? cause.message : 'Le retour terrain n’a pas pu être enregistré. Réessaie dans un instant.'
}
