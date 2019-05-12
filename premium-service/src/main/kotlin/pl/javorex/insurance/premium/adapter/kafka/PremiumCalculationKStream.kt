package pl.javorex.insurance.premium.adapter.kafka

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.premium.application.ProposalAcceptedListener
import pl.javorex.kafka.streams.event.newEventStream
import java.util.*

internal class PremiumCalculationKStream(
        private val bootstrapServers: String,
        private val insuranceTopic: String,
        private val proposalAcceptedListener: ProposalAcceptedListener
) {
    private val props= Properties()
    private lateinit var streams: KafkaStreams

    init {
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "premium-service"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers

        init()
    }

    private fun init() {
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

        streamBuilder.newEventStream(insuranceTopic)
                .filter{ _, eventEnvelope -> eventEnvelope.isTypeOf(InsuranceCreationStarted::class.java)}
                .foreach{ _, eventEnvelope ->
                    val createInsurance = eventEnvelope.unpack(InsuranceCreationStarted::class.java)
                    proposalAcceptedListener.onProposalAccepted(createInsurance, eventEnvelope.aggregateVersion)
                }


        return streamBuilder.build()
    }
}