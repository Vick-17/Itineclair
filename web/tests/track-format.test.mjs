import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  formatCoverage,
  formatDistance,
  formatElevationRange,
  formatMaximumGrades,
  formatMeters,
} from '../src/track/track-format.ts'

const facts = {
  distanceMeters: 12_450.5,
  elevationGainMeters: 850,
  elevationLossMeters: 810,
  minimumElevationMeters: 1_020,
  maximumElevationMeters: 1_870,
  maximumUphillGradePercent: 18.4,
  maximumDownhillGradePercent: 21.2,
  gradeMinimumRunMeters: 25,
}

test('formats trace facts in compact French units', () => {
  assert.equal(formatDistance(850), '850 m')
  assert.equal(formatDistance(12_450.5), '12,5 km')
  assert.equal(formatMeters(850), '850 m')
  assert.equal(formatMeters(null), '—')
  assert.equal(formatElevationRange(facts), '1 020–1 870 m')
  assert.equal(formatMaximumGrades(facts), '+18,4 % / −21,2 %')
})

test('states partial elevation coverage without hiding it', () => {
  assert.equal(
    formatCoverage({
      pointCount: 120,
      segmentCount: 2,
      elevationPointCount: 90,
      elevationComplete: false,
    }),
    'Altitude partielle 90/120 · 2 segments',
  )
})
