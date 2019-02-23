package pl.javorex.insurance.creation.application

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.TimeWindows
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.util.kafka.streams.event.EventEnvelopeSerde
import pl.javorex.util.kafka.streams.event.newEventStream
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct

@Service
class PolicyCreationSagaStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
        @Value("\${kafka.topic.premium-events}") private val premiumEventsTopic: String,
        @Value("\${kafka.topic.policy-events}") private val policyEventsTopic: String,
        @Value("\${kafka.topic.new-policy-saga}") private val newPolicySagaTopic: String
) {
    val props= Properties()
    init {
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "policy-service"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
    }

    @PostConstruct
    fun createTopology(): Topology? {
        val streamBuilder = StreamsBuilder()

//        streamBuilder.createStreamOf(proposalEventsTopic)
//                .filter { _, event ->
//                    event.isTypeOf(ProposalAcceptedEvent::class.java) }
//                .to(newPolicySagaTopic, Produced.with(Serdes.String(), EventEnvelopeSerde()))
//
//        streamBuilder.createStreamOf(premiumEventsTopic)
//                .filter { _, event ->
//                    event.isTypeOf(PremiumCalculatedEvent::class.java) }
//                .to(newPolicySagaTopic, Produced.with(Serdes.String(), EventEnvelopeSerde()))

//        streamBuilder.createStreamOf(newPolicySagaTopic)
//                .groupByKey()
//                .windowedBy(
//                        TimeWindows
//                                .of(Duration.ofSeconds(25))
//                                .advanceBy(Duration.ofSeconds(5))
//                                .grace(Duration.ZERO)
//                )
//                .aggregate(
//                        { Builder() },
//                        { key, event, sagaBuilder ->
//                            println("PEEK $key $event")
//                            sagaBuilder.mergeEvent(event)
//                        },
//                        Materialized.`as`<String, Builder, WindowStore<Bytes, ByteArray>>("new-policy-saga-store")
//                                .withKeySerde(Serdes.StringSerde())
//                                .withValueSerde(JsonPojoSerde(Builder::class.java))
//                )
//                .toStream()
//                .filter { _, sagaBuilder -> !sagaBuilder.isCorrupted()}
//                .mapValues { sagaBuilder -> sagaBuilder.build() }
//                .map { key, saga -> KeyValue(key,  pack(key.key(), 1, saga)) }
//                .to(policyEventsTopic, Produced.with(
//                        WindowedSerdes.timeWindowedSerdeFrom(String::class.java),
//                        EventEnvelopeSerde()
//                ))


        return streamBuilder.build()
    }
}

