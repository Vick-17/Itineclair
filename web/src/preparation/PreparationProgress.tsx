import type { PreparationStep } from './preparation-flow'

const STEPS: ReadonlyArray<{
  id: PreparationStep
  number: string
  label: string
}> = [
  { id: 'route', number: '1', label: 'Trace' },
  { id: 'departure', number: '2', label: 'Départ' },
  { id: 'report', number: '3', label: 'Préparation' },
]

export function PreparationProgress({
  currentStep,
  reportAvailable,
  onStepChange,
}: {
  currentStep: PreparationStep
  reportAvailable: boolean
  onStepChange: (step: PreparationStep) => void
}) {
  const currentIndex = STEPS.findIndex((step) => step.id === currentStep)

  return (
    <nav
      className="preparation-progress"
      aria-label="Étapes de la préparation"
    >
      <ol>
        {STEPS.map((step, index) => {
          const reachable =
            index <= currentIndex
            || (step.id === 'report' && reportAvailable)
            || (step.id === 'departure' && reportAvailable)

          return (
            <li
              key={step.id}
              className={
                step.id === currentStep
                  ? 'preparation-progress-current'
                  : reachable
                    ? 'preparation-progress-complete'
                    : undefined
              }
            >
              <button
                type="button"
                onClick={() => onStepChange(step.id)}
                disabled={!reachable}
                aria-current={
                  step.id === currentStep ? 'step' : undefined
                }
              >
                <span>{step.number}</span>
                {step.label}
              </button>
            </li>
          )
        })}
      </ol>
    </nav>
  )
}
