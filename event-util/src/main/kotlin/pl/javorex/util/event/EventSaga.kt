package pl.javorex.util.event

val LACK_OF_EVENT = null

data class EventSaga(
        var timeout: Long = 0,
        var events: SagaEvents = SagaEvents(),
        var error: List<EventSagaError> = arrayListOf(),
        var creationTimestamp: Long = System.currentTimeMillis(),
        var finished: Boolean = false
) {

    fun mergeEvent(event: EventEnvelope): EventSaga {
        val eventType = event.eventType
        if (!events.contains(eventType)) {
            return this
        }

        if (events.isVersionDiffers(event.aggregateVersion)) {
            putError("Other request in progress", event.aggregateVersion)

            return this
        }

        if ((events.required.contains(eventType) && events.required[eventType] != LACK_OF_EVENT)
                || events.starting.contains(eventType) && events.starting[eventType] != LACK_OF_EVENT
        ) {
            putError("Double event $eventType")

            return this
        }

        if (isComplete()) {
            return this
        }

        events.collectOrHandleError(event) {
            putError(event.payload["error"].asText())
        }

        return this
    }

    fun finish() {
        finished = true
    }
    fun missingEvents() = events.required
            .filter { e -> e.value == LACK_OF_EVENT }
            .map { it.key }
            .toTypedArray()


    fun isTimeoutOccurred(timestamp: Long) =
            events.isStarted() && events.startedTimestamp + timeout < timestamp

    fun isExpired(timestamp: Long) =
            creationTimestamp + 2 * timeout < timestamp

    fun startedBefore(timestamp: Long): Boolean {
        val startedTimestamp = events.startedTimestamp

        return startedTimestamp < timestamp
    }

    fun startedBetween(from: Long, to: Long): Boolean {
        val startedTimestamp = events.startedTimestamp

        return startedTimestamp > from && startedTimestamp < to
    }

    fun isComplete() = events.starting.none { it.value == LACK_OF_EVENT }
            && events.required.none { it.value == LACK_OF_EVENT }

    fun hasErrors() = error.isNotEmpty()

    fun putError(message: String, version: Long = this.events.version) {
       error += EventSagaError(message, version)
    }

    fun takeErrors(): List<EventSagaError> {
        val takenErrors = error

        error = arrayListOf()

        return takenErrors
    }
}

private const val NO_VERSION = Long.MIN_VALUE

private const val EVENT_HAVENT_ARRIVED_YET = Long.MAX_VALUE
data class SagaEvents(
        var starting: HashMap<String, EventEnvelope?> = hashMapOf(),
        var required: HashMap<String, EventEnvelope?> = hashMapOf(),
        var expectedErrors: HashSet<String> = hashSetOf(),
        var startedTimestamp: Long = EVENT_HAVENT_ARRIVED_YET,
        var version: Long = NO_VERSION
) {
    fun contains(eventType: String) = starting.contains(eventType)
            || required.contains(eventType)
            || expectedErrors.contains(eventType)

    fun collectOrHandleError(event: EventEnvelope, onErrorConsumer: (EventEnvelope) -> Unit) {
        val eventType = event.eventType
        when {
            expectedErrors.contains(eventType) -> onErrorConsumer.invoke(event)
            starting.contains(eventType) -> {
                if (isStarted()) {
                    throw IllegalStateException("Saga already started for ${event.aggregateId}")
                }
                starting[eventType] = event
                startedTimestamp = event.timestamp
                version = event.aggregateVersion
            }
            required.contains(eventType) -> required[eventType] = event
        }
    }

    fun isVersionDiffers(otherVersion: Long) = version != NO_VERSION && version != otherVersion

    fun isStarted() = startedTimestamp != EVENT_HAVENT_ARRIVED_YET

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
}

data class EventSagaError(val message: String, val version: Long)