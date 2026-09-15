import type { TrackListStatus } from './tracks-api.ts'

export type TrackListStatusTone =
  | 'attention'
  | 'next'
  | 'review'
  | 'follow-up'
  | 'complete'

export type TrackListStatusDetails = {
  label: string
  description: string
  actionLabel: string
  symbol: string
  tone: TrackListStatusTone
}

const STATUS_DETAILS: Record<TrackListStatus, TrackListStatusDetails> = {
  ANALYSIS_PENDING: {
    label: 'Calcul à relancer',
    description: 'Ouvre cette sortie pour relancer le calcul de la trace.',
    actionLabel: 'Relancer le calcul',
    symbol: '!',
    tone: 'attention',
  },
  DEPARTURE_TO_PLAN: {
    label: 'Départ à renseigner',
    description: 'Ajoute la date, l’heure et la durée que tu prévois.',
    actionLabel: 'Continuer la préparation',
    symbol: '2',
    tone: 'next',
  },
  PREPARATION_TO_REVIEW: {
    label: 'Préparation à relire',
    description: 'Le départ est renseigné. Relis les points à vérifier.',
    actionLabel: 'Relire la préparation',
    symbol: '•',
    tone: 'review',
  },
  FEEDBACK_TO_RECORD: {
    label: 'Retour à ajouter',
    description: 'La période prévue est passée. Note ce qui s’est produit.',
    actionLabel: 'Ajouter le retour',
    symbol: '+',
    tone: 'follow-up',
  },
  FEEDBACK_RECORDED: {
    label: 'Retour enregistré',
    description: 'Ton retour d’expérience est conservé avec cette sortie.',
    actionLabel: 'Voir la sortie',
    symbol: '✓',
    tone: 'complete',
  },
}

export function trackListStatusDetails(
  status: TrackListStatus,
): TrackListStatusDetails {
  return STATUS_DETAILS[status]
}
