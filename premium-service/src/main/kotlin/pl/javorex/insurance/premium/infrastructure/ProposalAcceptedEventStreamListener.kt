package pl.javorex.insurance.premium.infrastructure

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.premium.application.ProposalAcceptedListener
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.kafka.streams.event.newEventStream
import java.util.*
import javax.annotation.PostConstruct

@Service
class ProposalAcceptedEventStreamListener(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
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
        val topology = createTopology(props)
        streams = KafkaStreams(topology, props)
        //streams.cleanUp()
        streams.start()

        Runtime.getRuntime()
                .addShutdownHook(
                        Thread(Runnable { streams.close() })
                )
    }

    fun createTopology(props: Properties): Topology {
        val streamBuilder = StreamsBuilder()

        streamBuilder.newEventStream(proposalEventsTopic)
                .filter{ _, eventEnvelope -> eventEnvelope.isTypeOf(ProposalAcceptedEvent::class.java)}
                .foreach{ _, eventEnvelope ->
                    val proposalEvent = eventEnvelope.unpack(ProposalAcceptedEvent::class.java)
                    proposalAcceptedListener.onProposalAccepted(proposalEvent, eventEnvelope.aggregateVersion)
                }


        return streamBuilder.build()
    }
}