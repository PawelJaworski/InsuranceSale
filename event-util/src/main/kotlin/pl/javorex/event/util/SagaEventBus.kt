package pl.javorex.event.util

interface SagaEventBus {
    fun emit(aggregateId: String, aggregateVersion: Long, event: Any)
    fun emitError(aggregateId: String, aggregateVersion: Long, event: Any)
}