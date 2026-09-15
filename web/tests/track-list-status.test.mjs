import assert from 'node:assert/strict'
import { test } from 'node:test'

import { trackListStatusDetails } from '../src/track/track-list-status.ts'

const statuses = [
  'ANALYSIS_PENDING',
  'DEPARTURE_TO_PLAN',
  'PREPARATION_TO_REVIEW',
  'FEEDBACK_TO_RECORD',
  'FEEDBACK_RECORDED',
]

test('gives every API status a visible label, explanation and next action', () => {
  for (const status of statuses) {
    const details = trackListStatusDetails(status)

    assert.ok(details.label.length > 0)
    assert.ok(details.description.length > 0)
    assert.ok(details.actionLabel.length > 0)
    assert.ok(details.symbol.length > 0)
  }
})

test('uses direct beginner-friendly wording for the preparation lifecycle', () => {
  assert.deepEqual(
    statuses.map((status) => trackListStatusDetails(status).label),
    [
      'Calcul à relancer',
      'Départ à renseigner',
      'Préparation à relire',
      'Retour à ajouter',
      'Retour enregistré',
    ],
  )
})

test('never presents a workflow state as a safety decision', () => {
  const copy = statuses
    .map((status) => Object.values(trackListStatusDetails(status)).join(' '))
    .join(' ')

  assert.doesNotMatch(copy, /sûre|sécurisée|sans risque|feu vert|prête/i)
})
