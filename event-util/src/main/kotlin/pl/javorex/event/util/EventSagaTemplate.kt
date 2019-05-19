package pl.javorex.event.util

import pl.javorex.event.saga.DoubleEvent
import pl.javorex.event.saga.OtherRequestAlreadyPending

val LACK_OF_EVENT = null

data class EventSagaTemplate(
        @PublishedApi
        internal val timeout: Long = 0,
        val events: SagaEvents = SagaEvents(),
        internal val expectedErrors: HashSet<String> = hashSetOf(),
        internal val errors: ArrayList<EventEnvelope> = arrayListOf(),
        internal val creationTimestamp: Long = System.currentTimeMillis()
) {

    fun mergeEvent(event: EventEnvelope) {
        val eventType = event.eventType
        val aggregateId = event.aggregateId
        val aggregateVersion = event.aggregateVersion
        when {
            !events.expects(eventType) && !expectedErrors.contains(eventType) ->
                return
            events.isVersionDiffers(aggregateVersion) ->
                putError(aggregateId, aggregateVersion, OtherRequestAlreadyPending())
            events.alreadyContains(eventType) ->
                putError(aggregateId, aggregateVersion, DoubleEvent(eventType))
            events.expects(eventType) ->
                events.collect(event)
            expectedErrors.contains(eventType) ->
                putError(event)
        }
    }

    fun isNotStarted() = !events.isStarted()

    fun startsWith(event: EventEnvelope) = events.starting.contains(event.eventType)

    fun isTimeoutOccurred(timestamp: Long) =
            events.isStarted() && events.startedTimestamp + timeout < timestamp

    fun isExpired(timestamp: Long) =
            creationTimestamp + 2 * timeout < timestamp

    fun isComplete() = events.containsAllRequired()

    fun hasErrors() = errors.isNotEmpty()

    fun takeErrors(): List<EventEnvelope> {
        val takenErrors = errors.toMutableList()

        errors.clear()
        return takenErrors
    }

    private fun putError(aggregateId: String, aggregateVersion: Long, event: Any) {
        errors += pack(aggregateId, aggregateVersion, event)
    }

    private fun putError(event: EventEnvelope) {
        errors += event
    }
}

private const val NO_VERSION = Long.MIN_VALUE

private const val EVENT_HAVENT_ARRIVED_YET = Long.MAX_VALUE
data class SagaEvents(
        @PublishedApi
        internal val starting: HashMap<String, EventEnvelope?> = hashMapOf(),
        @PublishedApi
        internal val required: HashMap<String, EventEnvelope?> = hashMapOf(),
        internal var startedTimestamp: Long = EVENT_HAVENT_ARRIVED_YET,
        internal var version: Long = NO_VERSION
) {
    fun collect(event: EventEnvelope) {
        val eventType = event.eventType
        when {
            starting.contains(eventType) -> {
                starting[eventType] = event
                startedTimestamp = event.timestamp
                version = event.aggregateVersion
            }
            required.contains(eventType) -> required[eventType] = event
        }
    }

    inline fun <reified T>get(event: Class<T>): T {
            val eventType = event.simpleName
            return when {
                starting.contains(eventType) ->
                    starting[eventType]!!.unpack(T::class.java)
                required.contains(eventType) ->
                    required[eventType]!!.unpack(T::class.java)
                else -> throw IllegalStateException("Cannot get event of type $eventType")
            }
    }

    fun missing() =
            required
                    .filter { e -> e.value == LACK_OF_EVENT }
                    .map { it.key }

    fun version() = version

    internal fun isStarted() = startedTimestamp != EVENT_HAVENT_ARRIVED_YET

    internal fun isVersionDiffers(otherVersion: Long) = version != NO_VERSION && version != otherVersion

    internal fun expects(eventType: String) = starting.contains(eventType)
            || required.contains(eventType)

    internal fun alreadyContains(eventType: String) =
            starting[eventType] != LACK_OF_EVENT || required[eventType] != LACK_OF_EVENT

    internal fun containsAllRequired() =
            starting.none{ it.value == LACK_OF_EVENT } && required.none{ it.value == LACK_OF_EVENT }
}