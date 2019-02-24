package pl.javorex.util.kafka.streams.event

import org.apache.kafka.common.serialization.Serde
import pl.javorex.util.event.EventEnvelope
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde

class EventEnvelopeSerde(
    private val s: JsonPojoSerde<EventEnvelope> = JsonPojoSerde(EventEnvelope::class.java)
) : Serde<EventEnvelope> by s