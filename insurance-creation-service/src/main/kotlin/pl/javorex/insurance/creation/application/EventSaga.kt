package pl.javorex.insurance.creation.application

import pl.javorex.util.event.EventEnvelope

private val LACK_OF_EVENT = null

class EventSaga(
        var version: SagaVersion = SagaVersion(),
        var errors: HashMap<Long, String> = hashMapOf(),
        var events: SagaEvents = SagaEvents()
) {

    fun startsWith(clazz: Class<*>): EventSaga {
        events.starting["${clazz.simpleName}"] = LACK_OF_EVENT

        return this
    }
    fun requires(clazz: Class<*>): EventSaga {
        events.required["${clazz.simpleName}"] = LACK_OF_EVENT

        return this
    }
    fun expectErrors(clazz: Class<*>): EventSaga {
        events.expectedErrors += clazz.simpleName

        return this
    }

    fun mergeEvent(event: EventEnvelope): EventSaga {
        val eventType = event.eventType
        check(events.contains(eventType)) {
            throw IllegalStateException("Unrecognized event of type $eventType")
        }

        val eventVersion = SagaVersion(event.aggregateVersion)
        if (version.isMoreCurrentThan(eventVersion)) {
            errors[eventVersion.number] = "Request outdated"
            return this
        }

        if (version.isLessCurrentThan(eventVersion)) {
            errors[version.number] = "Request outdated"
            version = eventVersion
        }

        if ((events.required.contains(eventType) && events.required[eventType] != LACK_OF_EVENT)
                || events.starting.contains(eventType) && events.starting[eventType] != LACK_OF_EVENT
        ) {
            errors[version.number] = "Double event $eventType"

            return this
        }

        if (isComplete()) {
            return this
        }

        events.collectOrHandleError(event) {
            errors[version.number] = event.payload["error"].asText()
        }

        return this
    }

    fun startedBefore(timestamp: Long): Boolean {
        val startedTimestamp = events.startedTimestamp

        return startedTimestamp != null && startedTimestamp!! < timestamp
    }

    fun isComplete() = events.starting.none { it.value == LACK_OF_EVENT}
            && events.required.none { it.value == LACK_OF_EVENT}

    fun hasErrors() = errors.isNotEmpty()

    fun takeErrors(): List<String> {
        val takenErrors = errors.keys
                .map { errors[it]!! }

        errors = hashMapOf()

        return takenErrors
    }
}

private const val SMALLEST_VERSION_NO = 0L
data class SagaVersion(var number: Long = SMALLEST_VERSION_NO) {
    init{
        check(number >= 0) {"Saga-version-number cannot be less than 1"}
    }
    fun isMoreCurrentThan(other: SagaVersion) = number > other.number
    fun isLessCurrentThan(other: SagaVersion) = number != SMALLEST_VERSION_NO && number < other.number
}
data class SagaEvents(
        val starting: HashMap<String, EventEnvelope?> = hashMapOf(),
        var startedTimestamp: Long? = null,
        val required: HashMap<String, EventEnvelope?> = hashMapOf(),
        val expectedErrors: HashSet<String> = hashSetOf()
) {
    fun contains(eventType: String) = starting.contains(eventType)
            || required.contains(eventType)
            || expectedErrors.contains(eventType)

    fun collectOrHandleError(event: EventEnvelope, onErrorConsumer: (EventEnvelope) -> Unit) {
        val eventType = event.eventType
        when {
            expectedErrors.contains(eventType) -> onErrorConsumer.invoke(event)
            starting.contains(eventType) -> {
                starting[eventType] = event
                startedTimestamp = event.timestamp
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
}
