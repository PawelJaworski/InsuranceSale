package pl.javorex.event.util

interface SagaEventFactory {
    fun newErrorEvent(
            aggregateId: String,
            aggregateVersion: Long,
            error: String
    ) : Any

    fun newTimeoutEvent(
            aggregateId: String,
            aggregateVersion: Long,
            missedEvents: List<String>
    ): Any
}