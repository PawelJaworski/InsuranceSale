package pl.javorex.kafka.streams.event

import pl.javorex.event.util.EventEnvelope

interface EventUniquenessListener {
    fun onFirst(event: EventEnvelope, eventBus: ProcessorEventBus)

    fun onUniqueViolated(error: EventEnvelope, eventBus: ProcessorEventBus)
}