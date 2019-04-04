package pl.javorex.insurance.creation.adapter

import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.*
import org.apache.kafka.streams.processor.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde
import pl.javorex.kafka.streams.event.EventEnvelopeSerde
import java.lang.Exception
import java.util.*
import javax.annotation.PostConstruct
import org.apache.kafka.streams.state.*
import pl.javorex.event.util.*
import pl.javorex.insurance.creation.application.InsuranceCreationSagaEventListener
import pl.javorex.insurance.creation.application.InsuranceCreationSagaTemplateFactory
import pl.javorex.kafka.streams.event.EventSagaProcessor
import pl.javorex.kafka.streams.event.HeartBeatInterval
import pl.javorex.kafka.streams.event.SinkType
import pl.javorex.kafka.streams.event.StoreType

@Service
class InsuranceCreationSagaEventStream(
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
        val storeSupplier: KeyValueBytesStoreSupplier = Stores
                .inMemoryKeyValueStore(SagaStores.INSURANCE_CREATION.storeName)
        val storeBuilder = Stores
                .keyValueStoreBuilder(storeSupplier, Serdes.String(), JsonPojoSerde(EventSagaTemplate::class.java))

        return StreamsBuilder().build()
                .addSource(SourceType.INSURANCE_EVENTS, insuranceCreationEvents)
                .addSource(SourceType.PREMIUM_EVENTS, premiumEventsTopic)
                .addProcessor(
                        SagaProcessors.INSURANCE_CREATION_SAGA.processorName,
                        ProcessorSupplier{
                            EventSagaProcessor(
                                    { InsuranceCreationSagaTemplateFactory.newSagaTemplate() },
                                    HeartBeatInterval.ofSeconds(2),
                                    SagaStores.INSURANCE_CREATION,
                                    InsuranceCreationSagaEventListener,
                                    SagaSinks.INSURANCE_CREATION,
                                    SagaSinks.INSURANCE_CREATION_ERROR
                            )
                        },
                        SourceType.INSURANCE_EVENTS.sourceName,
                        SourceType.PREMIUM_EVENTS.sourceName
                )
                .addStateStore(storeBuilder, SagaProcessors.INSURANCE_CREATION_SAGA.processorName)
                .addSink(
                        SagaSinks.INSURANCE_CREATION,
                        insuranceCreationEvents,
                        SagaProcessors.INSURANCE_CREATION_SAGA
                )
                .addSink(
                        SagaSinks.INSURANCE_CREATION_ERROR,
                        insuranceCreationErrorTopic,
                        SagaProcessors.INSURANCE_CREATION_SAGA
                );
    }
}

private fun Topology.addSource(sourceType: SourceType, topic: String): Topology {
    return this.addSource(sourceType.sourceName, sourceType.keyDeserializer, sourceType.valueDeserializer, topic)
}

private fun Topology.addSink(sinkType: SagaSinks, topic: String, parent: SagaProcessors): Topology {
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
    INSURANCE_EVENTS("Insurance-Events-Sourece"),
    PROPOSAL_EVENTS("Proposal-Events-Source"),
    PREMIUM_EVENTS("Premium-Events-Source")
}
private enum class SagaStores(override val storeName: String) : StoreType {
    INSURANCE_CREATION("Insurance-Creation-Saga-Store")
}
private enum class SagaProcessors(val processorName: String) {
    INSURANCE_CREATION_SAGA("Insurance-Creation-Saga-Processor")
}
private enum class SagaSinks(
        override val sinkName: String,
        val keySerializer: StringSerializer = StringSerializer(),
        val valueSerializer: Serializer<EventEnvelope> = EventEnvelopeSerde().serializer()
)  : SinkType {
    INSURANCE_CREATION("Insurance-Creation-Sink"),
    INSURANCE_CREATION_ERROR("Insurance-Creation-Error-Sink")
}
