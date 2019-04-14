package pl.javorex.event.util

fun unambiguousVersionKeyOf(event: EventEnvelope) =
        UnambiguousVersionKey(event.aggregateId, event.aggregateVersion)

val NON_EXISTENT = UnambiguousVersionKey("", Long.MIN_VALUE)
data class UnambiguousVersionKey(val aggregateId: String, val aggregateVersion: Long) {
    fun asString() = this.toString()
}