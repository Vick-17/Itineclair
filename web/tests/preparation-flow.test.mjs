import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  defaultPlannedStart,
  initialPreparationStep,
  normalizeLocalDateTime,
} from '../src/preparation/preparation-flow.ts'

test('starts a new preparation by checking the route', () => {
  assert.equal(initialPreparationStep(null), 'route')
})

test('opens an already planned outing on its preparation', () => {
  assert.equal(initialPreparationStep({ planned: true }), 'report')
})

test('builds a local default for tomorrow at eight', () => {
  const start = defaultPlannedStart(new Date(2026, 8, 11, 17, 30))

  assert.equal(start, '2026-09-12T08:00')
})

test('adds seconds only when the browser omitted them', () => {
  assert.equal(normalizeLocalDateTime('2026-09-12T08:00'), '2026-09-12T08:00:00')
  assert.equal(normalizeLocalDateTime('2026-09-12T08:00:30'), '2026-09-12T08:00:30')
})
