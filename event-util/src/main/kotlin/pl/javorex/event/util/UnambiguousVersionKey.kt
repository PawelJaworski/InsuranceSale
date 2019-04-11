package pl.javorex.event.util

fun unambiguousVersionKeyOf(event: EventEnvelope) =
        UnambiguousVersionKey(event.aggregateId, event.aggregateVersion).toString()
data class UnambiguousVersionKey(val aggregateId: String, val aggregateVersion: Long)