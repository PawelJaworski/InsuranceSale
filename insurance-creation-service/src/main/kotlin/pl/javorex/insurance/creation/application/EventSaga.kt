package pl.javorex.insurance.creation.application

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import pl.javorex.util.event.EventEnvelope
import java.time.Duration
import java.time.Instant

private val LACK_OF_EVENT = null

class EventSagaBuilder(
        private var events: SagaEvents = SagaEvents(),
        private var errors: HashMap<Long, String> = hashMapOf()
) {
    private var timeout: Long = 0

    fun withTimeout(timeout: Duration): EventSagaBuilder {
        this.timeout = timeout.toMillis()

        return this
    }

    fun startsWith(clazz: Class<*>): EventSagaBuilder {
        events.starting[clazz.simpleName] = LACK_OF_EVENT

        return this
    }
    fun requires(clazz: Class<*>): EventSagaBuilder {
        events.required[clazz.simpleName] = LACK_OF_EVENT

        return this
    }
    fun expectErrors(clazz: Class<*>): EventSagaBuilder {
        events.expectedErrors.add(clazz.simpleName)

        return this
    }

    fun build() : EventSaga {
        return EventSaga(timeout, events, errors)
    }
}

data class EventSaga(
        var timeout: Long = 0,
        var events: SagaEvents = SagaEvents(),
        var errors: HashMap<Long, String> = hashMapOf(),
        var creationTimestamp: Long = System.currentTimeMillis(),
        var version: SagaVersion = SagaVersion()
) {

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
data class SagaVersion(
        val number: Long = SMALLEST_VERSION_NO
) {
    init{
        check(number >= 0) {"Saga-version-number cannot be less than 0"}
    }
    fun isMoreCurrentThan(other: SagaVersion) = number > other.number
    fun isLessCurrentThan(other: SagaVersion) = number != SMALLEST_VERSION_NO && number < other.number
}

private const val EVENT_HAVENT_ARRIVED_YET = Long.MAX_VALUE
data class SagaEvents(
        var starting: HashMap<String, EventEnvelope?> = hashMapOf(),
        var required: HashMap<String, EventEnvelope?> = hashMapOf(),
        var expectedErrors: HashSet<String> = hashSetOf(),
        var startedTimestamp: Long = EVENT_HAVENT_ARRIVED_YET
) {
    fun isStarted() = startedTimestamp != EVENT_HAVENT_ARRIVED_YET

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
