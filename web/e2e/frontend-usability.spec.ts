import AxeBuilder from '@axe-core/playwright'
import {
  expect,
  test,
  type Page,
  type TestInfo,
} from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const PASSWORD = 'Phrase de passe pour le test navigateur 2026'
const GPX_FIXTURE = fileURLToPath(
  new URL('./fixtures/beginner-track.gpx', import.meta.url),
)

test('le clavier atteint le contenu et pilote les onglets de compte', async ({
  page,
}) => {
  await openGuestPage(page)

  await page.keyboard.press('Tab')
  const skipLink = page.getByRole('link', {
    name: 'Aller au contenu principal',
  })
  await expect(skipLink).toBeFocused()

  await page.keyboard.press('Enter')
  await expect(page.getByRole('main')).toBeFocused()

  await page.keyboard.press('Tab')
  const loginTab = page.getByRole('tab', { name: 'Se connecter' })
  await expect(loginTab).toBeFocused()

  await page.keyboard.press('ArrowRight')
  const registerTab = page.getByRole('tab', { name: 'Créer un compte' })
  await expect(registerTab).toBeFocused()
  await expect(registerTab).toHaveAttribute('aria-selected', 'true')
  await expect(
    page.getByRole('tabpanel', { name: 'Créer un compte' }),
  ).toBeVisible()

  const focusStyle = await registerTab.evaluate((element) => {
    const style = window.getComputedStyle(element)

    return {
      outlineStyle: style.outlineStyle,
      outlineWidth: Number.parseFloat(style.outlineWidth),
      boxShadow: style.boxShadow,
    }
  })

  expect(focusStyle.outlineStyle).not.toBe('none')
  expect(focusStyle.outlineWidth).toBeGreaterThanOrEqual(2)
  expect(focusStyle.boxShadow).not.toBe('none')

  await page.keyboard.press('Tab')
  await expect(page.getByLabel('Adresse e-mail')).toBeFocused()
  await page.keyboard.press('Shift+Tab')
  await expect(registerTab).toBeFocused()
})

test('les pages principales exposent une structure lisible aux aides techniques', async ({
  page,
}, testInfo) => {
  await openGuestPage(page)

  await expect(page.locator('html')).toHaveAttribute('lang', 'fr')
  await expect(page).toHaveTitle(
    'Se connecter ou créer un compte — Itinéclair',
  )
  await expect(page.getByRole('main')).toHaveCount(1)
  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Une trace n’est pas encore un plan.',
    }),
  ).toBeVisible()
  await assertNoAutomaticWcagViolations(page, testInfo, 'visiteur')

  await registerAccount(page)

  await expect(page).toHaveTitle('Accueil — Itinéclair')
  await expect(
    page.getByRole('navigation', { name: 'Navigation principale' })
      .getByRole('link', { name: 'Accueil' }),
  ).toHaveAttribute('aria-current', 'page')
  await assertNoAutomaticWcagViolations(page, testInfo, 'accueil')

  await desktopNavigation(page).getByRole('link', {
    name: 'Mes sorties',
  }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Mes sorties' }))
    .toBeVisible()
  await expect(page).toHaveTitle('Mes sorties — Itinéclair')
  await assertNoAutomaticWcagViolations(page, testInfo, 'sorties')

  await desktopNavigation(page).getByRole('link', {
    name: 'Compte',
  }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Compte' }))
    .toBeVisible()
  await expect(page).toHaveTitle('Mon compte — Itinéclair')
  await assertNoAutomaticWcagViolations(page, testInfo, 'compte')
})

test('un débutant va du fichier GPX au rapport sans choix implicite', async ({
  page,
}, testInfo) => {
  await openGuestPage(page)
  await registerAccount(page)

  await page.getByRole('button', { name: 'Préparer une sortie' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Mes sorties' }))
    .toBeVisible()

  const fileInput = page.locator('input[type="file"]')
  await expect(fileInput).toHaveAttribute('accept', /\.gpx/)
  await fileInput.setInputFiles(GPX_FIXTURE)
  await expect(page.getByText('beginner-track.gpx')).toBeVisible()

  const importButton = page.getByRole('button', { name: 'Ajouter la trace' })
  await expect(importButton).toBeEnabled()
  await importButton.click()

  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Vérifie que c’est le bon parcours.',
    }),
  ).toBeVisible()
  await expect(page).toHaveTitle(
    'Étape 1 sur 3 : Trace – Itinéclair',
  )
  await expect(page.getByText('Boucle du lac — test débutant')).toBeVisible()

  await page.getByRole('button', {
    name: 'Cette trace me convient',
  }).click()

  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Quand comptes-tu partir ?',
    }),
  ).toBeVisible()
  await page.getByLabel('Date et heure de départ')
    .fill(futureLocalDateTime())
  await page.getByLabel('Durée prévue').selectOption('240')

  const weatherConsent = page.getByRole('checkbox', {
    name: /Ajouter la météo du point de départ/,
  })
  await expect(weatherConsent).not.toBeChecked()

  await page.getByRole('button', { name: 'Voir ma préparation' }).click()

  await expect(page.getByRole('heading', {
    level: 1,
    name: 'Boucle du lac — test débutant',
  })).toBeVisible()
  await expect(page).toHaveTitle(
    'Étape 3 sur 3 : Préparation – Itinéclair',
  )
  await expect(page.getByText('À regarder d’abord')).toBeVisible()
  await expect(
    page.getByText(/Ce résumé n’est pas un feu vert\./),
  ).toBeVisible()
  await expect(page.getByText('Non demandée', { exact: true })).toBeVisible()

  await assertNoAutomaticWcagViolations(page, testInfo, 'rapport')
})

test('les tâches essentielles restent utilisables dans 320 pixels', async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 568 })
  await openGuestPage(page)
  await assertNoHorizontalOverflow(page)

  await registerAccount(page)
  await assertNoHorizontalOverflow(page)

  const primaryAction = page.getByRole('button', {
    name: 'Préparer une sortie',
  })
  await expectMinimumTargetSize(primaryAction, 44)

  const navigation = page.getByRole('navigation', {
    name: 'Navigation principale sur mobile',
  })

  for (const label of ['Accueil', 'Mes sorties', 'Compte']) {
    await expectMinimumTargetSize(
      navigation.getByRole('link', { name: label }),
      44,
    )
  }

  await navigation.getByRole('link', { name: 'Mes sorties' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Mes sorties' }))
    .toBeVisible()
  await assertNoHorizontalOverflow(page)

  await navigation.getByRole('link', { name: 'Compte' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Compte' }))
    .toBeVisible()
  await assertNoHorizontalOverflow(page)
})

async function openGuestPage(page: Page) {
  await page.goto('/')
  await expect(page.getByRole('tab', { name: 'Se connecter' })).toBeVisible()
}

async function registerAccount(page: Page) {
  const email = `usability-${Date.now()}-${randomUUID().slice(0, 8)}@example.test`

  await page.getByRole('tab', { name: 'Créer un compte' }).click()
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Phrase de passe', { exact: true }).fill(PASSWORD)
  await page.getByLabel('Confirmer la phrase de passe').fill(PASSWORD)
  await page.getByRole('button', { name: 'Créer mon compte' }).click()

  await expect(page.getByRole('heading', {
    level: 1,
    name: 'Préparer une sortie, sans chercher où commencer.',
  })).toBeVisible()
}

function desktopNavigation(page: Page) {
  return page.getByRole('navigation', { name: 'Navigation principale' })
}

async function assertNoAutomaticWcagViolations(
  page: Page,
  testInfo: TestInfo,
  pageName: string,
) {
  const results = await new AxeBuilder({ page })
    .withTags([
      'wcag2a',
      'wcag2aa',
      'wcag21a',
      'wcag21aa',
      'wcag22aa',
    ])
    .analyze()

  await testInfo.attach(`axe-${pageName}`, {
    body: JSON.stringify(results, null, 2),
    contentType: 'application/json',
  })

  expect(
    results.violations,
    formatAxeViolations(pageName, results.violations),
  ).toEqual([])
}

function formatAxeViolations(
  pageName: string,
  violations: Array<{
    id: string
    help: string
    nodes: Array<{ target: unknown }>
  }>,
): string {
  if (violations.length === 0) {
    return `Aucune violation automatique sur ${pageName}.`
  }

  return [
    `Violations automatiques sur ${pageName} :`,
    ...violations.map((violation) =>
      `- ${violation.id}: ${violation.help} (${violation.nodes
        .map((node) => JSON.stringify(node.target))
        .join(', ')})`),
  ].join('\n')
}

async function assertNoHorizontalOverflow(page: Page) {
  const dimensions = await page.evaluate(() => ({
    viewportWidth: document.documentElement.clientWidth,
    contentWidth: document.documentElement.scrollWidth,
  }))

  expect(dimensions.contentWidth).toBeLessThanOrEqual(
    dimensions.viewportWidth + 1,
  )
}

async function expectMinimumTargetSize(
  locator: ReturnType<Page['locator']>,
  minimumPixels: number,
) {
  await expect(locator).toBeVisible()
  const box = await locator.boundingBox()

  expect(box).not.toBeNull()
  expect(box?.width ?? 0).toBeGreaterThanOrEqual(minimumPixels)
  expect(box?.height ?? 0).toBeGreaterThanOrEqual(minimumPixels)
}

function futureLocalDateTime(): string {
  const future = new Date(Date.now() + 7 * 24 * 60 * 60 * 1_000)
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Paris',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(future)
  const values = Object.fromEntries(
    parts.map(({ type, value }) => [type, value]),
  )

  return `${values.year}-${values.month}-${values.day}T09:00`
}
