import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  readWorkspaceSection,
  workspaceHash,
} from '../src/workspace/workspace-route.ts'

test('reads the three private workspace sections', () => {
  assert.equal(readWorkspaceSection('#/home'), 'home')
  assert.equal(readWorkspaceSection('#/outings'), 'outings')
  assert.equal(readWorkspaceSection('#/account'), 'account')
})

test('falls back to home for empty, unknown and shared routes', () => {
  assert.equal(readWorkspaceSection(''), 'home')
  assert.equal(readWorkspaceSection('#/unknown'), 'home')
  assert.equal(readWorkspaceSection('#/share/token'), 'home')
})

test('builds stable hashes for browser navigation', () => {
  assert.equal(workspaceHash('home'), '#/home')
  assert.equal(workspaceHash('outings'), '#/outings')
  assert.equal(workspaceHash('account'), '#/account')
})
