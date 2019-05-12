package pl.javorex.event.util

interface SagaEventListener {
    fun onComplete(
            aggregateId: String,
            aggregateVersion: Long,
            events: SagaEvents,
            eventBus: SagaEventBus
    )

    fun onError(error: EventEnvelope, eventBus: SagaEventBus)

    fun onTimeout(
            aggregateId: String,
            aggregateVersion: Long,
            events: SagaEvents,
            eventBus: SagaEventBus
    )
}