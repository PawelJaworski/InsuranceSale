package pl.javorex.util.event

val LACK_OF_EVENT = null

data class EventSagaTemplate(
        @PublishedApi
        internal val timeout: Long = 0,
        val events: SagaEvents = SagaEvents(),
        internal val errors: ArrayList<EventSagaError> = arrayListOf(),
        internal val creationTimestamp: Long = System.currentTimeMillis()
) {

    fun mergeEvent(event: EventEnvelope) {
        val eventType = event.eventType
        val eventVersion = event.aggregateVersion
        when {
            !events.contains(eventType) ->
                return
            events.isVersionDiffers(eventVersion) ->
                putError("Other request pending", eventVersion)
            events.alreadyContains(eventType) ->
                putError("Double event of type $eventType")
            else ->
                events.collectOrHandleError(event) {
                    putError(event.payload["error"].asText())
                }
        }
    }

    fun isTimeoutOccurred(timestamp: Long) =
            events.isStarted() && events.startedTimestamp + timeout < timestamp

    fun isExpired(timestamp: Long) =
            creationTimestamp + 2 * timeout < timestamp

    fun isComplete() = events.containsAllRequired()

    fun hasErrors() = errors.isNotEmpty()

    fun takeErrors(): List<EventSagaError> {
        val takenErrors = errors.toMutableList()

        errors.clear()
        return takenErrors
    }

    private fun putError(message: String, version: Long = this.events.version) {
        errors += EventSagaError(message, version)
    }

}

private const val NO_VERSION = Long.MIN_VALUE

private const val EVENT_HAVENT_ARRIVED_YET = Long.MAX_VALUE
data class SagaEvents(
        @PublishedApi
        internal val starting: HashMap<String, EventEnvelope?> = hashMapOf(),
        @PublishedApi
        internal val required: HashMap<String, EventEnvelope?> = hashMapOf(),
        internal val expectedErrors: HashSet<String> = hashSetOf(),
        internal var startedTimestamp: Long = EVENT_HAVENT_ARRIVED_YET,
        internal var version: Long = NO_VERSION
) {
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
                    .toTypedArray()

    fun version() = version

    internal fun isStarted() = startedTimestamp != EVENT_HAVENT_ARRIVED_YET

    internal fun isVersionDiffers(otherVersion: Long) = version != NO_VERSION && version != otherVersion

    internal fun expectError(eventType: String) {
        expectedErrors += eventType
    }

    internal fun contains(eventType: String) = starting.contains(eventType)
            || required.contains(eventType)
            || expectedErrors.contains(eventType)

    internal fun alreadyContains(eventType: String) =
            starting[eventType] != LACK_OF_EVENT || required[eventType] != LACK_OF_EVENT

    internal fun containsAllRequired() =
            starting.none{ it.value == LACK_OF_EVENT} && required.none{ it.value == LACK_OF_EVENT }
}

data class EventSagaError(val message: String, val version: Long)