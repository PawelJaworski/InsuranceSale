package pl.javorex.insurance.creation.application

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.kstream.Suppressed.untilWindowCloses
import org.apache.kafka.streams.state.WindowStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.event.EventEnvelope
import pl.javorex.util.event.pack
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde
import pl.javorex.util.kafka.streams.event.EventEnvelopeSerde
import pl.javorex.util.kafka.streams.event.from
import pl.javorex.util.kafka.streams.event.newEventStream
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct
import kotlin.reflect.KClass

@Service
class InsuranceCreationSagaStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
        @Value("\${kafka.topic.premium-events}") private val premiumEventsTopic: String,
        @Value("\${kafka.topic.policy-events}") private val policyEventsTopic: String,
        @Value("\${kafka.topic.insurance-creation-saga-events}") private val insuranceCreationSagaTopic: String,
        @Value("\${kafka.topic.insurance-creation-error-events}") private val insuranceCreationErrorTopic: String
) {
    private val props= Properties()
    private lateinit var streams: KafkaStreams

    init {
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "policy-service"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
    }

    @PostConstruct
    fun init() {
        val topology = createTopology(props)
        streams = KafkaStreams(topology, props)
        streams.start()
    }

    fun createTopology(props: Properties): Topology {
        val streamBuilder = StreamsBuilder()

        val proposalEventStream = streamBuilder.newEventStream(proposalEventsTopic)
        val premiumEventStream = streamBuilder.newEventStream(premiumEventsTopic)

        PremiumCalculatedEvent::class from premiumEventStream to insuranceCreationSagaTopic

        val saga = proposalEventStream
                .through(
                        insuranceCreationSagaTopic,
                        Produced.with(Serdes.StringSerde(), EventEnvelopeSerde())
                )
                .groupByKey()
                .windowedBy(
                        TimeWindows
                                .of(Duration.ofSeconds(25))
                                .advanceBy(Duration.ofSeconds(5))
                                .grace(Duration.ZERO)
                )
                .aggregate(
                        { Builder() },
                        { _, event, sagaBuilder -> sagaBuilder.mergeEvent(event) },
                        Materialized
                                .`as`<String, Builder, WindowStore<Bytes, ByteArray>>("insurance-creation-saga-store")
                                .withKeySerde(Serdes.StringSerde())
                                .withValueSerde(JsonPojoSerde(Builder::class.java))
                )

        saga.toStream()
                .flatMapValues { sagaBuilder -> sagaBuilder.build() }
                .map { key, saga -> KeyValue(key.key(),  pack(key.key(), saga.version, saga)) }
                .to(policyEventsTopic, Produced.with(
                        Serdes.StringSerde(),
                        EventEnvelopeSerde()
                ))

        saga.suppress(untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream()
            .flatMapValues { sagaBuilder -> sagaBuilder.buildMissing() }
            .map { key, corruptedSaga -> KeyValue(key.key(), pack(key.key(), corruptedSaga.version, corruptedSaga)) }
            .to(insuranceCreationErrorTopic, Produced.with(
                Serdes.StringSerde(),
                EventEnvelopeSerde()
            ))

        saga.toStream()
            .flatMapValues { sagaBuilder -> sagaBuilder.buildCorrupted() }
            .map { key, corruptedSaga -> KeyValue(key,  pack(key.key(), corruptedSaga.version, corruptedSaga)) }
            .to(insuranceCreationErrorTopic, Produced.with(
                WindowedSerdes.timeWindowedSerdeFrom(String::class.java),
                EventEnvelopeSerde()
            ))

        return streamBuilder.build()
    }
}


