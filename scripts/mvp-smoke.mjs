import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";

const apiBaseUrl = (
  process.argv[2] ?? "http://127.0.0.1:8080/api"
).replace(/\/+$/, "");
const webBaseUrl = new URL(apiBaseUrl).origin;

const testId = `${Date.now()}-${randomUUID().slice(0, 8)}`;
const email = `mvp-smoke-${testId}@example.test`;
const password = "Phrase de passe réservée au smoke test 2026";
const sourceFilename = `itineclair-smoke-${testId}.gpx`;

class CookieJar {
  #cookies = new Map();
  #secureCookies = new Set();

  absorb(headers) {
    const values = typeof headers.getSetCookie === "function"
      ? headers.getSetCookie()
      : splitCombinedSetCookie(headers.get("set-cookie"));

    for (const value of values) {
      const [pair, ...attributes] = value.split(";");
      const separator = pair.indexOf("=");

      if (separator <= 0) {
        continue;
      }

      const name = pair.slice(0, separator).trim();
      const cookieValue = pair.slice(separator + 1).trim();
      const expired = cookieValue === ""
        || attributes.some((attribute) =>
          /^max-age\s*=\s*0$/i.test(attribute.trim()));

      if (expired) {
        this.#cookies.delete(name);
        this.#secureCookies.delete(name);
      } else {
        this.#cookies.set(name, cookieValue);

        if (attributes.some((attribute) =>
          /^secure$/i.test(attribute.trim()))) {
          this.#secureCookies.add(name);
        } else {
          this.#secureCookies.delete(name);
        }
      }
    }
  }

  header() {
    return [...this.#cookies.entries()]
      .map(([name, value]) => `${name}=${value}`)
      .join("; ");
  }

  decoded(name) {
    const value = this.#cookies.get(name);
    return value === undefined ? undefined : decodeURIComponent(value);
  }

  isSecure(name) {
    return this.#secureCookies.has(name);
  }
}

function splitCombinedSetCookie(value) {
  if (!value) {
    return [];
  }

  return value.split(/,(?=\s*[^;,=]+=[^;,]*)/);
}

const cookieJar = new CookieJar();

function step(message) {
  console.log(`→ ${message}`);
}

async function request(
  path,
  {
    expectedStatus = 200,
    withCookies = true,
    ...options
  } = {},
) {
  const headers = new Headers(options.headers);
  const cookieHeader = cookieJar.header();

  if (withCookies && cookieHeader) {
    headers.set("Cookie", cookieHeader);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers,
    redirect: "manual",
  });

  if (withCookies) {
    cookieJar.absorb(response.headers);
  }

  const acceptedStatuses = Array.isArray(expectedStatus)
    ? expectedStatus
    : [expectedStatus];

  if (!acceptedStatuses.includes(response.status)) {
    const body = (await response.text()).slice(0, 1_000);
    throw new Error(
      `${options.method ?? "GET"} ${path}: statut ${response.status}, `
      + `attendu ${acceptedStatuses.join(" ou ")} — ${body}`,
    );
  }

  return response;
}

async function refreshCsrfToken() {
  await request("/auth/csrf", { expectedStatus: 204 });
  const token = cookieJar.decoded("XSRF-TOKEN");

  assert.ok(token, "Le serveur n’a pas créé le cookie CSRF.");
  assert.ok(
    cookieJar.isSecure("XSRF-TOKEN"),
    "Le cookie CSRF de production doit porter l’attribut Secure.",
  );

  return token;
}

async function mutate(
  path,
  method,
  {
    json,
    body,
    expectedStatus = 200,
  } = {},
) {
  const csrfToken = await refreshCsrfToken();
  const headers = new Headers({
    "X-XSRF-TOKEN": csrfToken,
  });

  let requestBody = body;

  if (json !== undefined) {
    headers.set("Content-Type", "application/json");
    requestBody = JSON.stringify(json);
  }

  return request(path, {
    method,
    headers,
    body: requestBody,
    expectedStatus,
  });
}

function tomorrowInParisAtTen() {
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1_000);
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Paris",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(tomorrow);

  const values = Object.fromEntries(
    parts.map(({ type, value }) => [type, value]),
  );

  return `${values.year}-${values.month}-${values.day}T10:00:00`;
}

const gpx = `<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1"
     creator="itineclair-production-smoke"
     xmlns="http://www.topografix.com/GPX/1/1">
  <trk>
    <name>Trace éphémère du smoke test</name>
    <trkseg>
      <trkpt lat="46.3710" lon="6.4810">
        <ele>1000</ele>
        <time>2026-09-09T07:00:00Z</time>
      </trkpt>
      <trkpt lat="46.3720" lon="6.4820">
        <ele>1050</ele>
        <time>2026-09-09T07:10:00Z</time>
      </trkpt>
      <trkpt lat="46.3730" lon="6.4830">
        <ele>1020</ele>
        <time>2026-09-09T07:20:00Z</time>
      </trkpt>
    </trkseg>
  </trk>
</gpx>`;

async function main() {
  step("vérifier le frontal et ses en-têtes de sécurité");
  const page = await fetch(`${webBaseUrl}/`, { redirect: "manual" });

  assert.equal(page.status, 200);
  assert.equal(page.headers.get("x-content-type-options"), "nosniff");
  assert.equal(page.headers.get("x-frame-options"), "DENY");
  assert.match(
    page.headers.get("content-security-policy") ?? "",
    /default-src 'self'/,
  );
  assert.match(
    page.headers.get("permissions-policy") ?? "",
    /geolocation=\(\)/,
  );

  const html = await page.text();
  assert.match(html, /<html lang="fr">/);
  assert.match(html, /<title>Itinéclair/);

  step("vérifier la santé de l’API");
  const healthResponse = await request("/actuator/health");
  const health = await healthResponse.json();

  assert.equal(health.status, "UP");

  step("vérifier qu’une route privée refuse un visiteur anonyme");
  await request("/auth/me", { expectedStatus: 401 });

  step("créer puis authentifier un compte éphémère");
  await mutate("/auth/register", "POST", {
    expectedStatus: 201,
    json: { email, password },
  });

  const loginResponse = await mutate("/auth/login", "POST", {
    json: { email, password },
  });
  const authenticatedAccount = await loginResponse.json();

  assert.equal(authenticatedAccount.email, email);
  assert.ok(
    cookieJar.isSecure("ITINECLAIR_SESSION"),
    "Le cookie de session de production doit porter l’attribut Secure.",
  );

  step("enregistrer un profil privé minimal");
  const profileResponse = await mutate("/profile", "PUT", {
    json: {
      experienceLevel: "REGULAR",
      usualDurationMinutes: 360,
      usualDistanceMeters: 14_000,
      usualElevationGainMeters: 900,
    },
  });
  const profile = await profileResponse.json();

  assert.equal(profile.configured, true);
  assert.equal(profile.experienceLevel, "REGULAR");

  step("importer un GPX et calculer ses faits");
  const form = new FormData();

  form.append(
    "file",
    new Blob([gpx], { type: "application/gpx+xml" }),
    sourceFilename,
  );

  const importResponse = await mutate("/tracks", "POST", {
    body: form,
    expectedStatus: 201,
  });
  const track = await importResponse.json();

  assert.ok(track.id);
  assert.equal(track.sourceFilename, sourceFilename);
  assert.equal(track.pointCount, 3);
  assert.ok(track.facts.distanceMeters > 0);

  step("planifier la sortie sans transmettre de coordonnées à la météo");
  const outdoorResponse = await mutate(
    `/tracks/${track.id}/outdoor-context`,
    "PUT",
    {
      json: {
        plannedStartLocal: tomorrowInParisAtTen(),
        plannedDurationMinutes: 180,
        timeZone: "Europe/Paris",
        shareStartPointWithWeatherProvider: false,
      },
    },
  );
  const outdoorContext = await outdoorResponse.json();

  assert.equal(outdoorContext.planned, true);
  assert.equal(outdoorContext.weather.status, "NOT_REQUESTED");

  step("générer une analyse explicable avec ses limites");
  const analysisResponse = await request(`/tracks/${track.id}/analysis`);
  const analysis = await analysisResponse.json();

  assert.ok(analysis.ruleSetVersion >= 1);
  assert.ok(Array.isArray(analysis.findings));
  assert.ok(Array.isArray(analysis.checklist));
  assert.ok(analysis.limitations.length > 0);

  step("enregistrer un retour post-sortie privé");
  const feedbackResponse = await mutate(
    `/tracks/${track.id}/feedback`,
    "PUT",
    {
      json: {
        outcome: "COMPLETED_AS_PLANNED",
        actualDurationMinutes: 175,
        perceivedEffort: 3,
        conditionsComparison: "AS_EXPECTED",
        observedIssues: ["NAVIGATION"],
      },
    },
  );

  assert.equal(
    (await feedbackResponse.json()).outcome,
    "COMPLETED_AS_PLANNED",
  );

  step("contrôler l’aperçu puis créer un partage privé");
  const previewResponse = await request(`/tracks/${track.id}/share/preview`);
  const preview = await previewResponse.json();

  assert.ok(
    preview.privacy.excludedData.some((item) => item.includes("Profil")),
  );

  const shareResponse = await mutate(`/tracks/${track.id}/share`, "POST", {
    expectedStatus: 201,
    json: { durationDays: 1 },
  });
  const share = await shareResponse.json();

  assert.match(share.token, /^[A-Za-z0-9_-]{43}$/);

  step("lire le rapport partagé et vérifier l’absence de données privées");
  const publicResponse = await request("/shared-report", {
    withCookies: false,
    headers: {
      "X-Itineclair-Share-Token": share.token,
    },
  });

  assert.match(publicResponse.headers.get("cache-control") ?? "", /no-store/);
  assert.equal(publicResponse.headers.get("referrer-policy"), "no-referrer");
  assert.match(
    publicResponse.headers.get("x-robots-tag") ?? "",
    /noindex/,
  );

  const publicReport = await publicResponse.json();
  const serializedPublicReport = JSON.stringify(publicReport);

  assert.ok(!serializedPublicReport.includes(email));
  assert.ok(!serializedPublicReport.includes(sourceFilename));
  assert.ok(!serializedPublicReport.includes(share.token));
  assert.ok(!serializedPublicReport.includes('"latitude"'));
  assert.ok(!serializedPublicReport.includes('"longitude"'));
  assert.ok(!serializedPublicReport.includes('"observedIssues"'));
  assert.ok(!serializedPublicReport.includes('"actualDurationMinutes"'));
  assert.ok(!serializedPublicReport.includes('"experienceLevel"'));
  assert.equal(publicReport.feedback, undefined);
  assert.equal(publicReport.profile, undefined);
  assert.equal(publicReport.hikerProfile, undefined);

  step("exporter l’ensemble des données du compte");
  const exportResponse = await mutate("/account/export", "POST", {
    json: { currentPassword: password },
  });

  assert.match(
    exportResponse.headers.get("content-type") ?? "",
    /application\/zip/,
  );
  assert.match(
    exportResponse.headers.get("content-disposition") ?? "",
    /attachment/i,
  );
  assert.match(exportResponse.headers.get("cache-control") ?? "", /no-store/);

  const archive = new Uint8Array(await exportResponse.arrayBuffer());

  assert.ok(archive.length > 4);
  assert.deepEqual([...archive.slice(0, 2)], [0x50, 0x4b]);

  step("révoquer le partage et vérifier que le secret ne fonctionne plus");
  await mutate(`/tracks/${track.id}/share`, "DELETE", {
    expectedStatus: 204,
  });

  await request("/shared-report", {
    expectedStatus: 404,
    withCookies: false,
    headers: {
      "X-Itineclair-Share-Token": share.token,
    },
  });

  step("supprimer le compte et invalider la session");
  await mutate("/account", "DELETE", {
    expectedStatus: 204,
    json: {
      currentPassword: password,
      confirmationEmail: email,
    },
  });

  await request("/auth/me", { expectedStatus: 401 });

  console.log("✓ Parcours MVP de production validé de bout en bout.");
}

main().catch((error) => {
  console.error("✗ Smoke test du MVP échoué.");
  console.error(error instanceof Error ? error.stack : error);
  process.exitCode = 1;
});
