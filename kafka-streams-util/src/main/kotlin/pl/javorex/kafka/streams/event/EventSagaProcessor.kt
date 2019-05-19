package pl.javorex.kafka.streams.event

import org.apache.kafka.streams.processor.*
import org.apache.kafka.streams.state.KeyValueStore
import pl.javorex.event.util.*
import java.time.Duration

class EventSagaProcessor(
        private val sagaSupplier: () -> EventSagaTemplate,
        private val heartBeatInterval: HeartBeatInterval,
        private val storeName: String,
        private val eventListener: SagaEventListener,
        private val sinkType: String,
        private val errorSinkType: String
) : Processor<String, EventEnvelope> {
    private lateinit var store: KeyValueStore<String, EventSagaTemplate>
    private lateinit var eventBus: ProcessorEventBus

    override fun init(context: ProcessorContext) {
        store = context
                .getStateStore(storeName) as KeyValueStore<String, EventSagaTemplate>
        eventBus = ProcessorEventBus(context!!, sinkType, errorSinkType)
        context
                .schedule(heartBeatInterval.duration, PunctuationType.WALL_CLOCK_TIME, this::doHeartBeat)
    }

    override fun process(aggregateId: String, event: EventEnvelope?) {
        if (event == null) {
            return
        }

       val saga = store.get(aggregateId) ?: sagaSupplier()

        if (saga.isNotStarted() && !saga.startsWith(event)) {
            return
        }

       saga.mergeEvent(event)
       store.put(aggregateId, saga)

        when {
            saga.isComplete() -> {
                val events = saga.events
                val aggregateVersion = events.version()
                eventListener
                        .onComplete(aggregateId, aggregateVersion, events, eventBus)
                store.delete(aggregateId)
            }
            saga.hasErrors() -> {
                saga.takeErrors().forEach{
                    eventListener.onError(it, eventBus)
                }
                store.delete(aggregateId)
            }
        }
    }

    private fun doHeartBeat(timestamp: Long) {
        val toRemove = arrayListOf<String>()
        store.all().forEachRemaining{
            val aggregateId = it.key
            val saga = it.value

            if (saga.isExpired(timestamp)) {
                toRemove += aggregateId
            }

            if (saga.isTimeoutOccurred(timestamp)) {
               fireTimeoutEvent(aggregateId, saga)
                toRemove += aggregateId
            }

        }
        toRemove.forEach {
            store.delete(it)
        }
    }

    private fun fireTimeoutEvent(aggregateId: String, saga: EventSagaTemplate) {
        val aggregateVersion = saga.events.version()
        eventListener.onTimeout(aggregateId, aggregateVersion, saga.events, eventBus)
    }

    override fun close() {}
}

class HeartBeatInterval(val duration: Duration) {
    companion object {
        fun ofSeconds(sec: Long) : HeartBeatInterval {
            val duration = Duration.ofSeconds(sec)

            return HeartBeatInterval(duration)
        }
    }
}