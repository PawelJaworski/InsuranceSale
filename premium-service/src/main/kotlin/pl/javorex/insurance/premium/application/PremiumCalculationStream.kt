package pl.javorex.insurance.premium.application

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Produced
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.premium.application.command.CalculatePremiumCommand
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.event.pack
import pl.javorex.util.kafka.streams.event.EventEnvelopeSerde
import pl.javorex.util.kafka.streams.event.newEventStream
import java.util.*
import javax.annotation.PostConstruct

@Service
class PremiumCalculationStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
        @Value("\${kafka.topic.premium-events}") private val premiumEventsTopic: String,
        val commandHandlers: CommandHandlers
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
        streams.start()
    }

    fun createTopology(props: Properties): Topology {
        val streamBuilder = StreamsBuilder()

        streamBuilder.newEventStream(proposalEventsTopic)
                .filter{ _, eventEnvelope -> eventEnvelope.isTypeOf(ProposalAcceptedEvent::class.java)}
                .mapValues { v ->
                    val event = v.unpack(ProposalAcceptedEvent::class.java)
                    val command = CalculatePremiumCommand(v.aggregateId, v.aggregateVersion, event.insuranceProduct, event.numberOfPremiums)
                    val premiumEvent = commandHandlers.handle(command)
                    pack(v.aggregateId, v.aggregateVersion, premiumEvent)
                }
                .to(premiumEventsTopic, Produced.with(Serdes.StringSerde(), EventEnvelopeSerde()))

        return streamBuilder.build()
    }
}