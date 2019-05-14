package pl.javorex.kafka.streams.event

import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import pl.javorex.event.util.EventEnvelope
import pl.javorex.util.kafka.common.serialization.JsonPOJODeserializer
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde

class EventEnvelopeSerde(
    private val s: JsonPojoSerde<EventEnvelope> = JsonPojoSerde(EventEnvelope::class.java)
) : Serde<EventEnvelope> by s

class EventEnvelopeDeserializer(private val d: Deserializer<EventEnvelope> = JsonPOJODeserializer(EventEnvelope::class.java)) :
        Deserializer<EventEnvelope> by d