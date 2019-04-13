package pl.javorex.insurance.creation.application

import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.SagaEventBus
import pl.javorex.event.util.SagaEventListener
import pl.javorex.event.util.SagaEvents
import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.creation.domain.event.InsuranceCreated
import pl.javorex.insurance.creation.domain.event.InsuranceCreationRollback
import pl.javorex.insurance.creation.domain.event.InsuranceCreationSagaCorrupted
import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailed

internal object InsuranceCreationSagaEventListener : SagaEventListener {
    override fun onComplete(aggregateId: String, aggregateVersion: Long, events: SagaEvents, eventBus: SagaEventBus) {

        val event = InsuranceCreated(
                events.get(InsuranceCreationStarted::class.java),
                events.get(PremiumCalculationCompleted::class.java)
        )

        eventBus.emit(aggregateId, aggregateVersion, event)
    }

    override fun onError(error: EventEnvelope, eventBus: SagaEventBus) {
        val event = when {
            error.isTypeOf(PremiumCalculationFailed::class.java) ->
                error.unpack(PremiumCalculationFailed::class.java).error
            else -> {
                error.payload.toString()
            }
        }

        eventBus.emitError(error.aggregateId, error.aggregateVersion, InsuranceCreationRollback())
        eventBus.emitError(error.aggregateId, error.aggregateVersion, event)
    }

    override fun onTimeout(aggregateId: String, aggregateVersion: Long, events: SagaEvents, eventBus: SagaEventBus) {
        val missingEvents = events.missing().joinToString(",")
        val event = "Request Timeout. Missing $missingEvents"

        eventBus.emitError(aggregateId, aggregateVersion, InsuranceCreationRollback())
        eventBus.emitError(aggregateId, aggregateVersion, event)
    }
}