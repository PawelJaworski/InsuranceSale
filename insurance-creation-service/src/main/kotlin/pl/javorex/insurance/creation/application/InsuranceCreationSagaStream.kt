package pl.javorex.insurance.creation.application

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.event.EventEnvelope
import pl.javorex.util.event.pack
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde
import pl.javorex.util.kafka.streams.event.EventEnvelopeSerde
import pl.javorex.util.kafka.streams.event.from
import pl.javorex.util.kafka.streams.event.newEventStream
import java.lang.Exception
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct

const val TIME_WINDOW_IN_SEC = 10L
const val TIME_WINDOW_ADVANCED_BY_IN_SEC = 2L

@Service
class InsuranceCreationSagaStream(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String,
        @Value("\${kafka.topic.premium-events}") private val premiumEventsTopic: String,
        @Value("\${kafka.topic.policy-events}") private val policyEventsTopic: String,
        @Value("\${kafka.topic.insurance-creation-saga-events}") private val insuranceCreationSagaTopic: String,
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

        val premiumEventStream = streamBuilder.newEventStream(premiumEventsTopic)
        PremiumCalculatedEvent::class from premiumEventStream to insuranceCreationSagaTopic
        PremiumCalculationFailedEvent::class from premiumEventStream to insuranceCreationSagaTopic

        val sagaEvents = groupSagaEvents(
            streamBuilder.newEventStream(proposalEventsTopic)
        )
        processCompleted(sagaEvents)
        //processMissing(sagaEvents)
        processCorrupted(sagaEvents)

        return streamBuilder.build()
    }

    private fun groupSagaEvents(proposalEventStream: KStream<String, EventEnvelope>) =
        proposalEventStream
                .filter{ _, event -> event.isTypeOf(ProposalAcceptedEvent::class.java)}
                .through(
                        insuranceCreationSagaTopic,
                        Produced.with(Serdes.StringSerde(), EventEnvelopeSerde())
                )
                .groupByKey()
                .windowedBy(
                        TimeWindows
                                .of(Duration.ofSeconds(TIME_WINDOW_IN_SEC))
                                .advanceBy(Duration.ofSeconds(TIME_WINDOW_ADVANCED_BY_IN_SEC))
                                .grace(Duration.ZERO)
                )
                .aggregate(
                        {
                            EventSaga()
                                    .startsWith(ProposalAcceptedEvent::class.java)
                                    .requires(PremiumCalculatedEvent::class.java)
                                    .expectErrors(PremiumCalculationFailedEvent::class.java)
                        },
                        { _, event, saga -> saga.mergeEvent(event) },
                        Materialized.with(Serdes.StringSerde(), JsonPojoSerde(EventSaga::class.java))
                )
                .filter{ key, saga ->
                    val nextWindowStart = key.window().start() + TIME_WINDOW_ADVANCED_BY_IN_SEC * 1000
                    saga.startedBefore(nextWindowStart)
                }
    private fun processCompleted(sagaEventGroup: KTable<Windowed<String>, EventSaga>) {
        sagaEventGroup.toStream()
                .filter{ _, saga -> saga != null }
                .filter{ key, saga ->
                    val nextWindowStart = key.window().start() + TIME_WINDOW_ADVANCED_BY_IN_SEC * 1000
                    saga.startedBefore(nextWindowStart)
                }
                .filter{ _, saga -> saga.isComplete() }
                .mapValues {
                    saga ->
                        InsuranceCreationSagaCompleted(
                                saga.version.number,
                                saga.events.get(ProposalAcceptedEvent::class.java),
                                saga.events.get(PremiumCalculatedEvent::class.java)
                        )
                }
                .map { key, saga -> KeyValue(key.key(),  pack(key.key(), saga.version, saga)) }
                .to(policyEventsTopic, Produced.with(
                        Serdes.StringSerde(),
                        EventEnvelopeSerde()
                ))
    }

    private fun processCorrupted(sagaEventGroup: KTable<Windowed<String>, EventSaga>) {
        sagaEventGroup.toStream()
                .filter{ _, saga -> saga != null }
                .filter{ _, saga -> saga.hasErrors()}
                .flatMapValues { saga ->
                    saga.takeErrors()
                        .map { InsuranceCreationSagaCorrupted(saga.version.number, it) }
                }
                .map { key, corruptedSaga -> KeyValue(key.key(),  pack(key.key(), corruptedSaga.version, corruptedSaga)) }
                .peek{ key, value -> println("[PEEKING] $key $value")}
                .to(insuranceCreationErrorTopic, Produced.with(
                        Serdes.StringSerde(),
                        EventEnvelopeSerde()
                ))
    }
}


