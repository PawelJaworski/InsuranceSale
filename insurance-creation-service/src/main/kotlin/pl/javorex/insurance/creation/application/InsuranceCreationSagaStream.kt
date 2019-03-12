package pl.javorex.insurance.creation.application

import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.*
import org.apache.kafka.streams.processor.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde
import pl.javorex.util.kafka.streams.event.EventEnvelopeSerde
import java.lang.Exception
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct
import org.apache.kafka.streams.state.*
import pl.javorex.insurance.creation.adapter.EventSagaProcessor
import pl.javorex.insurance.creation.adapter.HeartBeatInterval
import pl.javorex.util.event.EventEnvelope

@Service
class InsuranceCreationSagaStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
        @Value("\${kafka.topic.premium-events}") private val premiumEventsTopic: String,
        @Value("\${kafka.topic.insurance-creation-events}") private val insuranceCreationEvents: String,
        @Value("\${kafka.topic.insurance-creation-error-events}") private val insuranceCreationErrorTopic: String
) {
    private val props= Properties()
    private lateinit var streams: KafkaStreams

    init {
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "policy-service"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = "2000"
    }

    @PostConstruct
    fun init() {
        val topology = createTopology(props)
        streams = KafkaStreams(topology, props)
        try {
            streams.cleanUp()
        } catch(e: Exception){}

        streams.start()

        Runtime.getRuntime()
                .addShutdownHook(
                        Thread(Runnable { streams.close() })
                )
    }

    fun createTopology(props: Properties): Topology {
        val streamBuilder = StreamsBuilder()

        val proposalSourceName = "Proposal-Events-Source"
        val premiumSourceName = "Premium-Events-Source"
        val insuranceCreationSagaStoreName = "Insurance-Creation-Saga-Store"

        val storeSupplier: KeyValueBytesStoreSupplier = Stores.inMemoryKeyValueStore(insuranceCreationSagaStoreName)
        val storeBuilder = Stores.keyValueStoreBuilder(storeSupplier, Serdes.String(), JsonPojoSerde(EventSaga::class.java))
                .withCachingDisabled()

        return streamBuilder.build()
                .addSource(SourceType.PROPOSAL_EVENTS, proposalEventsTopic)
                .addSource(SourceType.PREMIUM_EVENTS, premiumEventsTopic)
                .addProcessor(
                        ProcessorType.INSURANCE_CREATION_SAGA.processorName,
                        ProcessorSupplier(this::insuranceCreationSagaProcessor),
                        SourceType.PROPOSAL_EVENTS.sourceName,
                        SourceType.PREMIUM_EVENTS.sourceName
                )
                .addStateStore(storeBuilder, ProcessorType.INSURANCE_CREATION_SAGA.processorName)
                .addSink(
                        SinkType.INSURANCE_CREATION,
                        insuranceCreationEvents,
                        ProcessorType.INSURANCE_CREATION_SAGA
                )
                .addSink(
                        SinkType.INSURANCE_CREATION_ERROR,
                        insuranceCreationErrorTopic,
                        ProcessorType.INSURANCE_CREATION_SAGA
                );
    }

    private fun insuranceCreationSagaProcessor(): EventSagaProcessor {
        var eventSagaSupplier = {
            EventSagaBuilder()
                    .withTimeout(Duration.ofSeconds(4))
                    .startsWith(ProposalAcceptedEvent::class.java)
                    .requires(PremiumCalculatedEvent::class.java)
                    .expectErrors(PremiumCalculationFailedEvent::class.java)
                    .build()
        }
        return EventSagaProcessor(
                eventSagaSupplier,
                HeartBeatInterval.ofSeconds(2),
                StoreType.INSURANCE_CREATION.storeName
        )
    }
}

private fun Topology.addSource(sourceType: SourceType, topic: String): Topology {
    return this.addSource(sourceType.sourceName, sourceType.keyDeserializer, sourceType.valueDeserializer, topic)
}

private fun Topology.addSink(sinkType: SinkType, topic: String, parent: ProcessorType): Topology {
    return this.addSink(
            sinkType.sinkName,
            topic,
            sinkType.keySerializer,
            sinkType.valueSerializer,
            parent.processorName
    )
}

private enum class SourceType(
        val sourceName: String,
        val keyDeserializer: StringDeserializer = StringDeserializer(),
        val valueDeserializer: Deserializer<EventEnvelope> = EventEnvelopeSerde().deserializer()
) {
    PROPOSAL_EVENTS("Proposal-Events-Source"),
    PREMIUM_EVENTS("Premium-Events-Source")
}
private enum class StoreType(val storeName: String) {
    INSURANCE_CREATION("Insurance-Creation-Saga-Store")
}
private enum class ProcessorType(val processorName: String) {
    INSURANCE_CREATION_SAGA("Insurance-Creation-Saga-Processor")
}
private enum class SinkType(
        val sinkName: String,
        val keySerializer: StringSerializer = StringSerializer(),
        val valueSerializer: Serializer<EventEnvelope> = EventEnvelopeSerde().serializer()
) {
    INSURANCE_CREATION("Insurance-Creation-Sink"),
    INSURANCE_CREATION_ERROR("Insurance-Creation-Error-Sink")
}