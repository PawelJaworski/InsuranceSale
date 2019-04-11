package pl.javorex.insurance.premium.adapter

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.springframework.beans.factory.annotation.Value
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.repack
import pl.javorex.event.util.unambiguousVersionKeyOf
import pl.javorex.insurance.creation.domain.event.InsuranceCreationRollback
import pl.javorex.insurance.premium.application.ProposalAcceptedListener
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationRollback
import pl.javorex.kafka.streams.event.newEventStream
import java.util.*
import javax.annotation.PostConstruct

class RollbackKStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.premium-events}") private val premiumTopic: String,
        @Value("\${kafka.topic.insurance-creation-error-events}") private val insuranceErrorTopic: String,
        private val proposalAcceptedListener: ProposalAcceptedListener
) {
    private val props = Properties()
    private lateinit var streams: KafkaStreams

    init {
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "premium-service"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
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
                .groupBy{ _, event -> unambiguousVersionKeyOf(event)}
                .reduce{ _, newValue -> newValue }
        val premiums = streamBuilder.newEventStream(premiumTopic)
                .groupBy{ _, event -> unambiguousVersionKeyOf(event)}
                .reduce{ _, newValue -> newValue }
        premiums.filter{ _, v -> v.isTypeOf(PremiumCalculatedEvent::class.java)}
                .join(
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