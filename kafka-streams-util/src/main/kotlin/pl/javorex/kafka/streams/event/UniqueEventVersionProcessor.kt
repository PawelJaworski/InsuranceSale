package pl.javorex.kafka.streams.event

import org.apache.kafka.streams.processor.Processor
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.EventSagaTemplate

class UniqueEventVersionProcessor(
        private val storeName: String,
        private val eventListener: EventUniquenessListener,
        private val sinkName: String,
        private val errorSinkName: String
) : Processor<String, EventEnvelope> {
    private lateinit var store: KeyValueStore<String, EventEnvelope>
    private lateinit var eventBus: ProcessorEventBus

    override fun init(context: ProcessorContext) {
        store = context
                .getStateStore(storeName) as KeyValueStore<String, EventEnvelope>
        eventBus = ProcessorEventBus(context!!, sinkName, errorSinkName)
    }

    override fun process(aggregateId: String, event: EventEnvelope?) {
        if (event == null) {
            return
        }

        val key = uniqueKeyOf(event)
        val previous = store.get(key)
        if (previous != null) {
            eventListener.onUniqueViolated(event, eventBus)
        } else {
            store.put(key, event)
            eventListener.onFirst(event, eventBus)
        }
        store.all().forEachRemaining { println(" papapapa $it") }
    }

    override fun close(){}

    private fun uniqueKeyOf(event: EventEnvelope) = "unique-key-of-${event.aggregateId}-and-${event.aggregateVersion}"
}