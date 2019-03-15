package pl.javorex.event.util

interface SagaEventListener {
    fun onComplete(
            aggregateId: String,
            aggregateVersion: Long,
            events: SagaEvents,
            eventBus: SagaEventBus
    )

    fun onError(
            aggregateId: String,
            error: EventSagaError,
            eventBus: SagaEventBus
    )

    fun onTimeout(
            aggregateId: String,
            aggregateVersion: Long,
            events: SagaEvents,
            eventBus: SagaEventBus
    )
}