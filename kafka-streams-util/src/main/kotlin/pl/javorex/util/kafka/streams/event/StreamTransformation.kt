package pl.javorex.util.kafka.streams.event

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import pl.javorex.event.util.EventEnvelope
import kotlin.reflect.KClass

inline infix fun <reified T : Any> KClass<T>.from(source: KStream<String, EventEnvelope>): ToTopic {
    return ToTopic(
            source.filter { _, event -> event.isTypeOf(T::class.java) }
    )
}

class ToTopic(val from: KStream<String, EventEnvelope>) {
    infix fun to(topic: String) {
        from.to(topic, Produced.with(Serdes.String(), EventEnvelopeSerde()))
    }
}