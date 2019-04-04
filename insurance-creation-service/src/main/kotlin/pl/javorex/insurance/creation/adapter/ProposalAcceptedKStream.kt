package pl.javorex.insurance.creation.adapter

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.pack
import pl.javorex.event.util.repack
import pl.javorex.insurance.creation.domain.event.CreateInsuranceFromProposal
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.kafka.streams.event.*
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde
import java.lang.Exception
import java.util.*
import javax.annotation.PostConstruct

@Service
class ProposalAcceptedKStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
        @Value("\${kafka.topic.insurance-creation-events}") private val insuranceCreationEvents: String,
        @Value("\${kafka.topic.insurance-creation-error-events}") private val insuranceCreationErrorTopic: String
) {
        private val props= Properties()
        private lateinit var streams: KafkaStreams

        init {
            props[StreamsConfig.APPLICATION_ID_CONFIG] = "proposal-accepted-adapter"
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
            val streamBuilder = StreamsBuilder()
            val doublePrevent = streamBuilder
                    .newEventStream(proposalEventsTopic)
                    .filter{ _, v -> v.isTypeOf(ProposalAcceptedEvent::class.java)}
                    .groupBy ({ _: String, v:EventEnvelope -> EventEnvelopeKey(v.aggregateId, v.aggregateVersion)},
                            Grouped.with(
                                    JsonPojoSerde(EventEnvelopeKey::class.java),
                                    EventEnvelopeSerde()
                            ))
                    .aggregate(
                            {MaxOneEvent()},
                            { _, event, maxOneEvent -> maxOneEvent.add(event)},
                            Materialized.with(JsonPojoSerde(EventEnvelopeKey::class.java), JsonPojoSerde(MaxOneEvent::class.java))
                    )

                    doublePrevent
                    .toStream()
                            .filter{ _, v -> v == null}
                    .mapValues { k, _ -> pack(k.aggregateId, k.aggregateVersion, "Version already requested")}
                    .selectKey{ k, _ -> k.aggregateId}
                    .to(insuranceCreationErrorTopic)

            doublePrevent
                    .filter{ _, v -> v != null}
                    .mapValues { k, request ->
                        val p = request!!.event!!.unpack(ProposalAcceptedEvent::class.java)
                        repack(request.event!!, CreateInsuranceFromProposal(p.proposalId, p.insuranceProduct, p.numberOfPremiums))

                    }
                    .toStream()

                    .selectKey{ k, v -> k.aggregateId}
                    .to(insuranceCreationEvents)

            return streamBuilder.build()
        }
}
data class EventEnvelopeKey(val aggregateId: String, val aggregateVersion: Long)
class MaxOneEvent(val event: EventEnvelope? = null) {
    fun add(event: EventEnvelope?): MaxOneEvent? {
        if (this.event != null) {
            return null
        }

        return MaxOneEvent(event)
    }
}