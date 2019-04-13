package pl.javorex.insurance.premium.adapter

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.repack
import pl.javorex.event.util.unambiguousVersionKeyOf
import pl.javorex.insurance.creation.domain.event.InsuranceCreationRollback
import pl.javorex.insurance.premium.application.ProposalAcceptedListener
import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted
import pl.javorex.insurance.premium.domain.event.PremiumCalculationRollback
import pl.javorex.kafka.streams.event.EventEnvelopeSerde
import pl.javorex.kafka.streams.event.newEventStream
import java.util.*
import javax.annotation.PostConstruct

class RollbackKStream(
        private val bootstrapServers: String,
        private val premiumTopic: String,
        private val insuranceErrorTopic: String,
        private val proposalAcceptedListener: ProposalAcceptedListener
) {
    private val props = Properties()
    private lateinit var streams: KafkaStreams

    init {
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "premium-service-rollback"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = "2000"
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.StringSerde::class.java
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = EventEnvelopeSerde::class.java
    }

    @PostConstruct
    fun init() {
        val topology = createTopology()
        streams = KafkaStreams(topology, props)
        //streams.cleanUp()
        streams.start()

        Runtime.getRuntime()
                .addShutdownHook(
                        Thread(Runnable { streams.close() })
                )
    }

    private fun createTopology(): Topology {
        val streamBuilder = StreamsBuilder()

        val insuranceErrors = streamBuilder.newEventStream(insuranceErrorTopic)
                .filter{ _, event -> event.isTypeOf(InsuranceCreationRollback::class.java)}
                .groupBy({ _, event -> unambiguousVersionKeyOf(event)})
                .reduce{ _, newValue -> newValue }
        val premiums = streamBuilder.newEventStream(premiumTopic)
                .filter{ _, v -> v.isTypeOf(PremiumCalculationCompleted::class.java)}
                .groupBy{ _, event -> unambiguousVersionKeyOf(event)}
                .reduce{ _, newValue -> newValue }
        premiums.join(
                insuranceErrors
        ) {
            premium: EventEnvelope, _: EventEnvelope? -> repack(premium, PremiumCalculationRollback())
        }
                .toStream()
                .foreach{ _, event ->
                    println("performing rollback for $event")
                }



        return streamBuilder.build()
    }
}