import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  buildReportSummary,
  prioritizeChecklist,
  prioritizeFindings,
} from '../src/track/report-hierarchy.ts'

const finding = (code, severity) => ({
  code,
  category: 'WEATHER',
  severity,
  title: code,
  explanation: 'Explication',
  action: 'Action',
  evidence: [],
})

const checklistItem = (code, status) => ({
  code,
  status,
  title: code,
  detail: 'Détail',
})

const analysis = (findings, checklist) => ({
  ruleSetVersion: 3,
  reviewStatus: 'PROTOTYPE_AWAITING_EXPERT_REVIEW',
  generatedAt: '2026-09-14T08:00:00Z',
  sourceSnapshot: {
    factsVersion: 1,
    outdoorContextUpdatedAt: null,
    weatherCheckedAt: null,
  },
  findings,
  checklist,
  limitations: [],
})

test('puts the strongest findings first without mutating the API response', () => {
  const input = [
    finding('notice', 'NOTICE'),
    finding('strong', 'STRONG_CAUTION'),
    finding('caution', 'CAUTION'),
  ]

  assert.deepEqual(
    prioritizeFindings(input).map((item) => item.code),
    ['strong', 'caution', 'notice'],
  )
  assert.deepEqual(input.map((item) => item.code), ['notice', 'strong', 'caution'])
})

test('puts incomplete checks before available information', () => {
  const input = [
    checklistItem('available', 'AVAILABLE'),
    checklistItem('partial', 'PARTIAL'),
    checklistItem('verify', 'TO_VERIFY'),
  ]

  assert.deepEqual(
    prioritizeChecklist(input).map((item) => item.code),
    ['verify', 'partial', 'available'],
  )
})

test('summarises strong cautions and remaining checks without a score', () => {
  const summary = buildReportSummary(analysis(
    [finding('strong', 'STRONG_CAUTION'), finding('notice', 'NOTICE')],
    [checklistItem('verify', 'TO_VERIFY'), checklistItem('done', 'AVAILABLE')],
  ))

  assert.equal(summary.tone, 'strong')
  assert.equal(summary.title, '1 point demande une attention renforcée')
  assert.equal(summary.findingCount, 2)
  assert.equal(summary.remainingCheckCount, 1)
  assert.doesNotMatch(`${summary.title} ${summary.description}`, /score|prête|sûre|feu vert/i)
})

test('never turns the absence of triggered thresholds into reassurance', () => {
  const summary = buildReportSummary(analysis([], [
    checklistItem('verify-weather', 'TO_VERIFY'),
    checklistItem('verify-terrain', 'PARTIAL'),
  ]))

  assert.equal(summary.tone, 'neutral')
  assert.equal(summary.title, 'Aucun seuil automatique déclenché')
  assert.match(summary.description, /ne signifie pas que la sortie est sûre/i)
  assert.equal(summary.remainingCheckCount, 2)
})
