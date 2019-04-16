package pl.javorex.insurance.creation.application

import org.springframework.stereotype.Service
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.SagaEventBus
import pl.javorex.event.util.SagaEventListener
import pl.javorex.event.util.SagaEvents
import pl.javorex.insurance.creation.domain.event.*
import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailed

internal class InsuranceCreationSagaEventListener(
        private val insuranceFactory: InsuranceFactory
) : SagaEventListener {
    override fun onComplete(aggregateId: String, aggregateVersion: Long, events: SagaEvents, eventBus: SagaEventBus) {

        val insuranceCreated = insuranceFactory.createInsurance(
                events.get(InsuranceCreationStarted::class.java),
                events.get(PremiumCalculationCompleted::class.java)
        )

        eventBus.emit(aggregateId, aggregateVersion, insuranceCreated)
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