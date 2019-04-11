package pl.javorex.insurance.creation.application

import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.SagaEventBus
import pl.javorex.event.util.SagaEventListener
import pl.javorex.event.util.SagaEvents
import pl.javorex.insurance.creation.domain.event.CreateInsurance
import pl.javorex.insurance.creation.domain.event.InsuranceCreated
import pl.javorex.insurance.creation.domain.event.InsuranceCreationRollback
import pl.javorex.insurance.creation.domain.event.InsuranceCreationSagaCorrupted
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent

internal object InsuranceCreationSagaEventListener : SagaEventListener {
    override fun onComplete(aggregateId: String, aggregateVersion: Long, events: SagaEvents, eventBus: SagaEventBus) {

        val event = InsuranceCreated(
                events.get(CreateInsurance::class.java),
                events.get(PremiumCalculatedEvent::class.java)
        )

        eventBus.emit(aggregateId, aggregateVersion, event)
    }

    override fun onError(error: EventEnvelope, eventBus: SagaEventBus) {
        val event = when {
            error.isTypeOf(PremiumCalculationFailedEvent::class.java) -> {
                val premiumCalculationFailed = error.unpack(PremiumCalculationFailedEvent::class.java)
                InsuranceCreationSagaCorrupted(premiumCalculationFailed.error)
            } else -> {
                val errorMessage = error.payload.toString()
                InsuranceCreationSagaCorrupted(errorMessage)
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