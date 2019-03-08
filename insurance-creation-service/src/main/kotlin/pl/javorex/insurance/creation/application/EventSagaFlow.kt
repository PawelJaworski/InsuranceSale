package pl.javorex.insurance.creation.application

import pl.javorex.util.event.EventEnvelope

private val LACK_OF_EVENT = null

class EventSagaFlow(
        var startedTimestamp: Long? = null,
        var version: SagaVersion = SagaVersion(SMALLEST_VERSION_NO),
        var errors: MutableMap<Long, String> = hashMapOf(),
        var events: SagaEvents = SagaEvents(),
        var terminated: Boolean = false
) {

    fun withSubEvent(clazz: Class<*>): EventSagaFlow {
        events.mandatory["${clazz.simpleName}"] = LACK_OF_EVENT

        return this
    }
    fun expectingErrors(clazz: Class<*>): EventSagaFlow {
        events.expectedErrors["${clazz.simpleName}"] = LACK_OF_EVENT

        return this
    }

    fun mergeEvent(event: EventEnvelope): EventSagaFlow {
        val eventType = event.eventType
        check(!events.contains(eventType)) {
            throw IllegalStateException("Unrecognized event of type $eventType")
        }

        val eventVersion = SagaVersion(event.aggregateVersion)
        if (version.isMoreCurrentThan(eventVersion)) {
            errors[eventVersion.number] += "Request outdated"
            return this
        }

        if (version.isLessCurrentThan(eventVersion)) {
            errors[version.number] += "Request outdated"
            version = eventVersion
        }

        if (events.mandatory.contains(eventType) && events.mandatory[eventType] != LACK_OF_EVENT) {
            errors[version.number] += "Double event $eventType"

            return this
        }

        if (events.expectedErrors.contains(eventType)) {
            errors[version.number] += "${event.unpack(Class.forName(eventType))}"
        } else {
            events.mandatory[eventType] = event
        }
        return this
    }

    fun isComplete() = events.mandatory.none { it.value == LACK_OF_EVENT}

    fun hasErrors() = errors.isNotEmpty()

    fun terminate() {
        terminated = true
    }

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
    fun isMoreCurrentThan(otherVersion: SagaVersion) = number > otherVersion.number
    fun isLessCurrentThan(otherVersion: SagaVersion) = number < otherVersion.number
}
data class SagaEvents(
        var mandatory: HashMap<String, EventEnvelope?> = hashMapOf(),
        var expectedErrors: HashMap<String, EventEnvelope?> = hashMapOf()
) {
    fun contains(eventType: String) = mandatory.contains(eventType) || expectedErrors.containsKey(eventType)
    inline fun <reified T>get(event: Class<T>): T =
            mandatory["${event.simpleName}"]!!.unpack(T::class.java)
}
