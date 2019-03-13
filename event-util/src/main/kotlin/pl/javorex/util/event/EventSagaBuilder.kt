package pl.javorex.util.event

import java.time.Duration

class EventSagaBuilder(
        private var events: SagaEvents = SagaEvents()
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
        return EventSaga(timeout, events)
    }
}