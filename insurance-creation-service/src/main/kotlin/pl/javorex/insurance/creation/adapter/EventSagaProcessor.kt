package pl.javorex.insurance.creation.adapter

import org.apache.kafka.streams.processor.*
import org.apache.kafka.streams.state.KeyValueStore
import pl.javorex.insurance.creation.application.EventSaga
import pl.javorex.insurance.creation.application.InsuranceCreationSagaCorrupted
import pl.javorex.util.event.EventEnvelope
import pl.javorex.util.event.pack
import java.time.Duration

class EventSagaProcessor(
        private val sagaSupplier: () -> EventSaga,
        private val heartBeatInterval: HeartBeatInterval,
        private val storeName: String
) : AbstractProcessor<String, EventEnvelope>() {
    private lateinit var store: KeyValueStore<String, EventSaga>
    private lateinit var streamTimeSchedule: Cancellable
    private lateinit var systemTimeSchedule: Cancellable

    override fun init(context: ProcessorContext?) {
        super.init(context)
        store = context().getStateStore(storeName) as KeyValueStore<String, EventSaga>

        systemTimeSchedule = context()
                .schedule(heartBeatInterval.milis, PunctuationType.WALL_CLOCK_TIME, this::doHeartBeat)
    }

    override fun process(key: String?, event: EventEnvelope?) {
        if (event == null) {
            return
        }

        val newSaga = sagaSupplier()
        val saga = store.putIfAbsent(key, newSaga)
        if (saga == null) {
            newSaga.mergeEvent(event)
            store.put(key, newSaga)

        } else {
            saga.mergeEvent(event)
            store.put(key, saga)
        }
    }

    private fun doHeartBeat(timestamp: Long) {
        val toRemove = arrayListOf<String>()
        store.all().forEachRemaining{
            val aggregateId = it.key
            val saga = it.value

            if (saga.isTimeoutOccurred(timestamp)) {
                fireTimeoutEvent(aggregateId, saga)
                toRemove += aggregateId
            }
            if (saga.isExpired(timestamp)) {
                toRemove += aggregateId
            }
        }
        toRemove.forEach {
            store.delete(it)
        }
    }

    private fun fireTimeoutEvent(aggregateId: String, saga: EventSaga) {
        val event = InsuranceCreationSagaCorrupted(saga.version.number, "Request timeout. Missing ${saga.missingEvents().contentToString()}")
        val eventEnvelope =  pack(aggregateId, saga.version.number, event)


        context().forward(aggregateId, eventEnvelope, To.child("Insurance-Creation-Error-Sink"))
    }
}

class HeartBeatInterval(val milis: Long) {
    companion object {
        fun ofSeconds(sec: Long) : HeartBeatInterval {
            val intervalInMilis = Duration.ofSeconds(sec).toMillis()

            return HeartBeatInterval(intervalInMilis)
        }
    }
}
