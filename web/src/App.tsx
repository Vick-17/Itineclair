import {
  useEffect,
  useId,
  useState,
  type FormEvent,
} from 'react'

import './App.css'
import {
  currentAccount,
  login,
  register,
  type Account,
} from './auth/auth-api'
import { ApiError } from './api/api-client'
import { TrackDashboard } from './track/TrackDashboard'
import { SharedReportPage } from './sharing/SharedReportPage'
import { readShareRoute } from './sharing/share-route'

type SessionState =
  | { status: 'loading' }
  | { status: 'guest' }
  | { status: 'authenticated'; account: Account }

type AuthMode = 'login' | 'register'

function App() {
  const [shareRoute, setShareRoute] = useState(
    () => readShareRoute(window.location.hash),
  )
  const [session, setSession] = useState<SessionState>({ status: 'loading' })
  const [bootstrapMessage, setBootstrapMessage] = useState<string | null>(null)

  useEffect(() => {
    function handleHashChange() {
      setShareRoute(readShareRoute(window.location.hash))
    }

    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  useEffect(() => {
    if (shareRoute.matched) {
      return
    }

    let active = true

    currentAccount()
      .then((account) => {
        if (!active) {
          return
        }

        setSession(
          account
            ? { status: 'authenticated', account }
            : { status: 'guest' },
        )
      })
      .catch((error: unknown) => {
        if (!active) {
          return
        }

        setBootstrapMessage(messageForError(error))
        setSession({ status: 'guest' })
      })

    return () => {
      active = false
    }
  }, [shareRoute.matched])

  if (shareRoute.matched) {
    return <SharedReportPage token={shareRoute.token} />
  }

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Aller au contenu principal
      </a>

      <Header session={session} />

      <main id="main-content">
        {session.status === 'loading' && <LoadingScreen />}

        {session.status === 'guest' && (
          <GuestLanding
            bootstrapMessage={bootstrapMessage}
            onAuthenticated={(account) => {
              setBootstrapMessage(null)
              setSession({ status: 'authenticated', account })
            }}
          />
        )}

        {session.status === 'authenticated' && (
          <TrackDashboard
            onLoggedOut={() => setSession({ status: 'guest' })}
          />
        )}
      </main>

      <Footer />
    </div>
  )
}

function Header({ session }: { session: SessionState }) {
  return (
    <header className="topbar">
      <a className="brand" href="/" aria-label="Itinéclair, accueil">
        <BrandMark />
        <span>Itinéclair</span>
      </a>

      <p className="topbar-tagline">Préparer. Comprendre. Décider.</p>

      {session.status === 'authenticated' && (
        <div className="account-chip" title={session.account.email}>
          <span className="account-avatar" aria-hidden="true">
            {session.account.email.slice(0, 1).toUpperCase()}
          </span>
          <span className="account-email">{session.account.email}</span>
        </div>
      )}
    </header>
  )
}

function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg viewBox="0 0 40 40">
        <path d="M5 29 14.5 13l5.2 8.1L24 15l11 14H5Z" />
        <path d="m11.6 29 5.6-9.4 2.5 3.9 4.4-6.2L33.3 29H11.6Z" />
      </svg>
    </span>
  )
}

function LoadingScreen() {
  return (
    <section className="loading-screen" aria-live="polite" aria-busy="true">
      <span className="loading-mark" aria-hidden="true" />
      <p>Vérification de ta session…</p>
    </section>
  )
}

function GuestLanding({
  bootstrapMessage,
  onAuthenticated,
}: {
  bootstrapMessage: string | null
  onAuthenticated: (account: Account) => void
}) {
  const [mode, setMode] = useState<AuthMode>('login')

  return (
    <div className="guest-layout">
      <section className="hero-copy" aria-labelledby="hero-title">
        <p className="eyebrow">
          <span aria-hidden="true">●</span>
          Copilote de préparation montagne
        </p>

        <h1 id="hero-title">
          Une trace n’est pas
          <span> encore un plan.</span>
        </h1>

        <p className="hero-intro">
          Itinéclair transforme ton itinéraire en faits lisibles, points de
          vigilance et questions concrètes avant le départ.
        </p>

        <ul className="benefit-list" aria-label="Ce qu’apporte Itinéclair">
          <li>
            <FeatureIcon kind="trace" />
            <span>
              <strong>Lis ta trace</strong>
              Distance, dénivelé, pente et altitude sans jargon inutile.
            </span>
          </li>
          <li>
            <FeatureIcon kind="weather" />
            <span>
              <strong>Croise le contexte</strong>
              Météo, lumière et contraintes réunies dans un même rapport.
            </span>
          </li>
          <li>
            <FeatureIcon kind="decision" />
            <span>
              <strong>Garde la décision</strong>
              Une aide explicable, jamais une promesse de sécurité.
            </span>
          </li>
        </ul>

        <div className="safety-note">
          <span aria-hidden="true">i</span>
          <p>
            Itinéclair aide à préparer une sortie. Il ne remplace ni
            l’expérience, ni les sources officielles, ni le jugement sur le
            terrain.
          </p>
        </div>
      </section>

      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="auth-heading">
          <p className="auth-kicker">Ton espace de préparation</p>
          <h2 id="auth-title">
            {mode === 'login' ? 'Bon retour parmi nous' : 'Créer ton compte'}
          </h2>
          <p>
            {mode === 'login'
              ? 'Retrouve tes analyses et prépare ta prochaine sortie.'
              : 'Commence avec une adresse e-mail et une phrase de passe.'}
          </p>
        </div>

        <div className="auth-tabs" role="tablist" aria-label="Accès au compte">
          <button
            type="button"
            role="tab"
            aria-selected={mode === 'login'}
            onClick={() => setMode('login')}
          >
            Se connecter
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={mode === 'register'}
            onClick={() => setMode('register')}
          >
            Créer un compte
          </button>
        </div>

        {bootstrapMessage && (
          <div className="form-alert form-alert-neutral" role="status">
            {bootstrapMessage}
          </div>
        )}

        <AuthForm
          key={mode}
          mode={mode}
          onAuthenticated={onAuthenticated}
        />
      </section>
    </div>
  )
}

function AuthForm({
  mode,
  onAuthenticated,
}: {
  mode: AuthMode
  onAuthenticated: (account: Account) => void
}) {
  const formId = useId()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [pending, setPending] = useState(false)
  const [formMessage, setFormMessage] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const isRegistration = mode === 'register'
  const emailErrorId = `${formId}-email-error`
  const passwordHintId = `${formId}-password-hint`
  const passwordErrorId = `${formId}-password-error`
  const confirmationErrorId = `${formId}-confirmation-error`

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormMessage(null)
    setFieldErrors({})

    if (isRegistration && password !== confirmation) {
      setFieldErrors({
        confirmation: 'Les deux phrases de passe doivent être identiques.',
      })
      return
    }

    setPending(true)

    try {
      if (isRegistration) {
        await register(email, password)
      }

      const account = await login(email, password)
      onAuthenticated(account)
    } catch (error: unknown) {
      if (error instanceof ApiError && error.violations.length > 0) {
        setFieldErrors(
          Object.fromEntries(
            error.violations.map((violation) => [
              violation.field,
              violation.message,
            ]),
          ),
        )
      }

      setFormMessage(messageForError(error))
    } finally {
      setPending(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      {formMessage && (
        <div className="form-alert" role="alert">
          <span aria-hidden="true">!</span>
          <p>{formMessage}</p>
        </div>
      )}

      <div className="form-field">
        <label htmlFor={`${formId}-email`}>Adresse e-mail</label>
        <input
          id={`${formId}-email`}
          name="email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          autoComplete={isRegistration ? 'email' : 'username'}
          autoCapitalize="none"
          spellCheck={false}
          required
          maxLength={254}
          aria-invalid={Boolean(fieldErrors.email)}
          aria-describedby={fieldErrors.email ? emailErrorId : undefined}
          disabled={pending}
        />
        {fieldErrors.email && (
          <p className="field-error" id={emailErrorId}>
            {fieldErrors.email}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor={`${formId}-password`}>Phrase de passe</label>
        <div className="password-input">
          <input
            id={`${formId}-password`}
            name="password"
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete={isRegistration ? 'new-password' : 'current-password'}
            required
            minLength={isRegistration ? 15 : undefined}
            maxLength={128}
            aria-invalid={Boolean(fieldErrors.password)}
            aria-describedby={
              [
                isRegistration ? passwordHintId : null,
                fieldErrors.password ? passwordErrorId : null,
              ]
                .filter(Boolean)
                .join(' ') || undefined
            }
            disabled={pending}
          />
          <button
            type="button"
            className="password-toggle"
            aria-pressed={showPassword}
            aria-label={
              showPassword
                ? 'Masquer la phrase de passe'
                : 'Afficher la phrase de passe'
            }
            onClick={() => setShowPassword((visible) => !visible)}
          >
            {showPassword ? 'Masquer' : 'Afficher'}
          </button>
        </div>
        {isRegistration && (
          <p className="field-hint" id={passwordHintId}>
            15 caractères minimum. Les espaces et caractères spéciaux sont
            acceptés.
          </p>
        )}
        {fieldErrors.password && (
          <p className="field-error" id={passwordErrorId}>
            {fieldErrors.password}
          </p>
        )}
      </div>

      {isRegistration && (
        <div className="form-field">
          <label htmlFor={`${formId}-confirmation`}>
            Confirmer la phrase de passe
          </label>
          <input
            id={`${formId}-confirmation`}
            name="confirmation"
            type={showPassword ? 'text' : 'password'}
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            autoComplete="new-password"
            required
            minLength={15}
            maxLength={128}
            aria-invalid={Boolean(fieldErrors.confirmation)}
            aria-describedby={
              fieldErrors.confirmation ? confirmationErrorId : undefined
            }
            disabled={pending}
          />
          {fieldErrors.confirmation && (
            <p className="field-error" id={confirmationErrorId}>
              {fieldErrors.confirmation}
            </p>
          )}
        </div>
      )}

      <button className="primary-button" type="submit" disabled={pending}>
        {pending && <span className="button-spinner" aria-hidden="true" />}
        {pending
          ? 'Connexion sécurisée…'
          : isRegistration
            ? 'Créer mon compte'
            : 'Me connecter'}
      </button>

      <p className="privacy-copy">
        Tes données de préparation restent privées. Aucun mot de passe n’est
        conservé en clair.
      </p>
    </form>
  )
}

function FeatureIcon({ kind }: { kind: 'trace' | 'weather' | 'decision' }) {
  return (
    <span className="feature-icon" aria-hidden="true">
      {kind === 'trace' && (
        <svg viewBox="0 0 24 24">
          <path d="M5 18.5 9.2 13l3.1 3.2L18.8 7" />
          <circle cx="5" cy="18.5" r="1.7" />
          <circle cx="18.8" cy="7" r="1.7" />
        </svg>
      )}
      {kind === 'weather' && (
        <svg viewBox="0 0 24 24">
          <path d="M7.2 17.5h10.1a3.2 3.2 0 0 0 .3-6.4 5.3 5.3 0 0 0-10.2 1.6h-.2a2.4 2.4 0 0 0 0 4.8Z" />
          <path d="M10 20h4" />
        </svg>
      )}
      {kind === 'decision' && (
        <svg viewBox="0 0 24 24">
          <path d="M12 3.5 19 6v5.4c0 4.2-2.8 7.6-7 9.1-4.2-1.5-7-4.9-7-9.1V6l7-2.5Z" />
          <path d="m8.8 12 2.1 2.1 4.4-4.5" />
        </svg>
      )}
    </span>
  )
}

function Footer() {
  return (
    <footer className="site-footer">
      <p>Itinéclair · Aide prudente à la préparation montagne</p>
      <p>Les conditions réelles peuvent changer rapidement.</p>
    </footer>
  )
}

function messageForError(error: unknown): string {
  if (error instanceof ApiError) {
    if (
      error.code === 'login_rate_limited' &&
      error.retryAfterSeconds !== undefined
    ) {
      return `${error.message} Attends environ ${formatDuration(error.retryAfterSeconds)}.`
    }

    return error.message
  }

  return 'Le service est momentanément inaccessible. Vérifie ta connexion puis réessaie.'
}

function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${seconds} seconde${seconds > 1 ? 's' : ''}`
  }

  const minutes = Math.ceil(seconds / 60)
  return `${minutes} minute${minutes > 1 ? 's' : ''}`
}

export default App
