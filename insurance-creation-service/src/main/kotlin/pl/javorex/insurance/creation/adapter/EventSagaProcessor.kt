package pl.javorex.insurance.creation.adapter

import org.apache.kafka.streams.processor.*
import org.apache.kafka.streams.state.KeyValueStore
import pl.javorex.event.util.EventSagaTemplate
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.SagaEventListener
import pl.javorex.event.util.pack
import java.time.Duration

class EventSagaProcessor(
        private val sagaSupplier: () -> EventSagaTemplate,
        private val heartBeatInterval: HeartBeatInterval,
        private val storeType: StoreType,
        private val eventListener: SagaEventListener,
        private val successSinkType: SinkType,
        private val errorSinkType: SinkType
) : AbstractProcessor<String, EventEnvelope>() {
    private lateinit var store: KeyValueStore<String, EventSagaTemplate>

    override fun init(context: ProcessorContext?) {
        super.init(context)
        store = context().getStateStore(storeType.storeName) as KeyValueStore<String, EventSagaTemplate>

        context()
                .schedule(heartBeatInterval.duration, PunctuationType.WALL_CLOCK_TIME, this::doHeartBeat)
    }

    override fun process(key: String?, event: EventEnvelope?) {
        if (event == null) {
            return
        }

       val newSaga = store.get(key) ?: sagaSupplier()
       newSaga.mergeEvent(event)

       if (newSaga.hasErrors()) {
           fireErrors(key!!, newSaga)
           store.delete(key)
       } else {
           store.put(key, newSaga)
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

    private fun fireErrors(aggregateId: String, saga: EventSagaTemplate) {
        val aggregateVersion = saga.events.version()
        saga.takeErrors().forEach{
            val event = eventListener.newErrorEvent(aggregateId, aggregateVersion, it.message)

            val eventEnvelope = pack(aggregateId, it.version, event)
            context().forward(aggregateId, eventEnvelope, To.child(errorSinkType.sinkName))
        }
    }

    private fun fireTimeoutEvent(aggregateId: String, saga: EventSagaTemplate) {
        val aggregateVersion = saga.events.version()
        val event = eventListener.newTimeoutEvent(aggregateId, aggregateVersion, saga.events.missing())

        val eventEnvelope = pack(aggregateId, aggregateVersion, event)
        context().forward(aggregateId, eventEnvelope, To.child(errorSinkType.sinkName))
    }
}

class HeartBeatInterval(val duration: Duration) {
    companion object {
        fun ofSeconds(sec: Long) : HeartBeatInterval {
            val duration = Duration.ofSeconds(sec)

            return HeartBeatInterval(duration)
        }
    }
}

interface StoreType {
    val storeName: String
}

interface SinkType {
    val sinkName: String
}

