package pl.javorex.insurance.premium.adapter

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.premium.application.ProposalAcceptedListener
import pl.javorex.kafka.streams.event.newEventStream
import java.util.*
import javax.annotation.PostConstruct

@Service
class PremiumCalculationKStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.insurance-creation-events}") private val insuranceCreationTopic: String,
        val proposalAcceptedListener: ProposalAcceptedListener
) {
    private val props= Properties()
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

    fun createTopology(): Topology {
        val streamBuilder = StreamsBuilder()

        streamBuilder.newEventStream(insuranceCreationTopic)
                .filter{ _, eventEnvelope -> eventEnvelope.isTypeOf(InsuranceCreationStarted::class.java)}
                .foreach{ _, eventEnvelope ->
                    val createInsurance = eventEnvelope.unpack(InsuranceCreationStarted::class.java)
                    proposalAcceptedListener.onProposalAccepted(createInsurance, eventEnvelope.aggregateVersion)
                }


        return streamBuilder.build()
    }
}