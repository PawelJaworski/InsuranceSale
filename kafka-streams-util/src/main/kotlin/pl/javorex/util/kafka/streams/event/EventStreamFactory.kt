package pl.javorex.util.kafka.streams.event

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import pl.javorex.util.event.EventEnvelope

typealias EventStream = KStream<String, EventEnvelope>

fun StreamsBuilder.newEventStream(topic: String): EventStream {
    return this.stream(
            topic,
            Consumed.with(
                    Serdes.String(),
                    EventEnvelopeSerde()
            )
    )
}
fun StreamsBuilder.newEventTable(topic: String): KTable<String, EventEnvelope> {
    return this.table(
            topic,
            Consumed.with(
                    Serdes.String(),
                    EventEnvelopeSerde()
            )
    )
}