package pl.javorex.insurance.creation.adapter

import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.*
import org.apache.kafka.streams.processor.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde
import java.lang.Exception
import java.util.*
import javax.annotation.PostConstruct
import org.apache.kafka.streams.state.*
import pl.javorex.event.util.*
import pl.javorex.insurance.creation.adapter.kafka.*
import pl.javorex.insurance.creation.application.InsuranceCreationSagaEventListener
import pl.javorex.insurance.creation.application.UniqueProposalAcceptedEventVersionListener
import pl.javorex.insurance.creation.application.newSagaTemplate
import pl.javorex.kafka.streams.event.*

@Service
internal class InsuranceCreationSagaEventStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
        @Value("\${kafka.topic.premium-events}") private val premiumEventsTopic: String,
        @Value("\${kafka.topic.insurance-creation-events}") private val insuranceCreationEvents: String,
        @Value("\${kafka.topic.insurance-creation-error-events}") private val insuranceCreationErrorTopic: String,
        private val insuranceCreationSagaEventListener: InsuranceCreationSagaEventListener
) {
    private val props= Properties()
    private lateinit var streams: KafkaStreams

    init {
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "policy-service"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = "2000"
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.StringSerde::class.java
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = EventEnvelopeSerde::class.java
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
                .persistentKeyValueStore(INSURANCE_CREATION_STORE)
        val storeBuilder = Stores
                .keyValueStoreBuilder(storeSupplier, Serdes.String(), JsonPojoSerde(EventSagaTemplate::class.java))
        val proposalAcceptedUniqueEventStoreSupplier: KeyValueBytesStoreSupplier = Stores
                .persistentKeyValueStore(UNIQUE_PROPOSAL_ACCEPTED_STORE)
        val proposalAcceptedUniqueEventStoreBuilder = Stores
                .keyValueStoreBuilder(proposalAcceptedUniqueEventStoreSupplier, Serdes.String(), EventEnvelopeSerde())

        return StreamsBuilder().build()
                .addSource(PROPOSAL_EVENTS_SOURCE, proposalEventsTopic)
                .addSource(INSURANCE_EVENTS_SOURCE, insuranceCreationEvents)
                .addSource(PREMIUM_EVENTS_SOURCE, premiumEventsTopic)
                .addProcessor(
                        PROPOSAL_ACCEPTED_UNIQUE_EVENT_PROCESSOR,
                        ProcessorSupplier(this::uniqueProposalAcceptedVersionProcessor),
                        PROPOSAL_EVENTS_SOURCE
                )
                .addProcessor(
                        INSURANCE_CREATION_SAGA_PROCESSOR,
                        ProcessorSupplier(this::newInsuranceCreationSagaProcessor),
                        INSURANCE_EVENTS_SOURCE,
                        PREMIUM_EVENTS_SOURCE
                )
                .addStateStore(storeBuilder, INSURANCE_CREATION_SAGA_PROCESSOR)
                .addStateStore(proposalAcceptedUniqueEventStoreBuilder, PROPOSAL_ACCEPTED_UNIQUE_EVENT_PROCESSOR)
                .addSink(
                        INSURANCE_CREATION_SINK,
                        insuranceCreationEvents,
                        PROPOSAL_ACCEPTED_UNIQUE_EVENT_PROCESSOR,
                        INSURANCE_CREATION_SAGA_PROCESSOR
                )
                .addSink(
                        INSURANCE_CREATION_ERROR_SINK,
                        insuranceCreationErrorTopic,
                        PROPOSAL_ACCEPTED_UNIQUE_EVENT_PROCESSOR,
                        INSURANCE_CREATION_SAGA_PROCESSOR
                );
    }

    private fun uniqueProposalAcceptedVersionProcessor() = UniqueEventVersionProcessor(
        UNIQUE_PROPOSAL_ACCEPTED_STORE,
        UniqueProposalAcceptedEventVersionListener,
        INSURANCE_CREATION_SINK,
        INSURANCE_CREATION_ERROR_SINK
    )

    private fun newInsuranceCreationSagaProcessor() = EventSagaProcessor(
            { newSagaTemplate() },
            HeartBeatInterval.ofSeconds(2),
            INSURANCE_CREATION_STORE,
            insuranceCreationSagaEventListener,
            INSURANCE_CREATION_SINK,
            INSURANCE_CREATION_ERROR_SINK
    )
}