import {
  useEffect,
  useState,
} from 'react'

import { TrackReport } from '../track/TrackReport'
import type {
  OutdoorContext,
  Track,
  TrackAnalysis,
} from '../track/tracks-api'
import { DepartureStep } from './DepartureStep'
import {
  initialPreparationStep,
  preparationStepDetails,
  type PreparationStep,
} from './preparation-flow'
import { PreparationProgress } from './PreparationProgress'
import { RouteCheckStep } from './RouteCheckStep'

type PreparationFlowProps = {
  track: Track
  outdoorContext: OutdoorContext | null
  analysis: TrackAnalysis
  onOutdoorContextChange: (context: OutdoorContext) => void
  onAnalysisChange: (analysis: TrackAnalysis) => void
  onUnauthorized: () => void
  onBack: () => void
}

export function PreparationFlow({
  track,
  outdoorContext,
  analysis,
  onOutdoorContextChange,
  onAnalysisChange,
  onUnauthorized,
  onBack,
}: PreparationFlowProps) {
  const [step, setStep] = useState<PreparationStep>(() =>
    initialPreparationStep(outdoorContext),
  )

  const reportAvailable = outdoorContext !== null

  useEffect(() => {
    const previousTitle = document.title
    const stepDetails = preparationStepDetails(step)

    document.title = `Étape ${stepDetails?.number ?? ''} sur 3 : ${stepDetails?.label ?? 'Préparation'} – Itinéclair`

    return () => {
      document.title = previousTitle
    }
  }, [step])

  return (
    <div className="preparation-flow">
      <PreparationProgress
        currentStep={step}
        reportAvailable={reportAvailable}
        onStepChange={setStep}
      />

      {step === 'route' && (
        <RouteCheckStep
          track={track}
          onBack={onBack}
          onContinue={() => setStep('departure')}
        />
      )}

      {step === 'departure' && (
        <DepartureStep
          track={track}
          outdoorContext={outdoorContext}
          onSaved={(savedContext, refreshedAnalysis) => {
            onOutdoorContextChange(savedContext)
            onAnalysisChange(refreshedAnalysis)
            setStep('report')
          }}
          onUnauthorized={onUnauthorized}
          onBack={() => setStep('route')}
        />
      )}

      {step === 'report' && (
        <TrackReport
          track={track}
          outdoorContext={outdoorContext}
          analysis={analysis}
          onEditDeparture={() => setStep('departure')}
          onUnauthorized={onUnauthorized}
          onBack={onBack}
        />
      )}
    </div>
  )
}
