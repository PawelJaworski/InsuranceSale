package pl.javorex.insurance.creation.application

import junit.framework.Assert.*
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.event.EventEnvelope
import pl.javorex.util.event.pack
import pl.javorex.util.kafka.streams.event.EventEnvelopeSerde
import java.math.BigDecimal
import java.time.Duration
import java.util.*

private const val PROPOSAL_ID = "proposal-1"
private const val BOOTSTRAP_SERVERS = "localhost:0000"
private const val PROPOSAL_EVENTS_TOPIC = "proposal-events-test"
private const val PREMIUM_EVENTS_TOPIC = "premium-events-test"
private const val POLICY_EVENTS_TOPIC = "policy-events-test"
private const val NEW_POLICY_SAGA_TOPIC = "policy-saga-test"

class PolicyCreationSagaStreamSpec {
    private lateinit var topologyTestDriver: TopologyTestDriver

    private val newPolicySaga = InsuranceCreationSagaStream(
            BOOTSTRAP_SERVERS,
            PROPOSAL_EVENTS_TOPIC,
            PREMIUM_EVENTS_TOPIC,
            POLICY_EVENTS_TOPIC,
            NEW_POLICY_SAGA_TOPIC
    )

    @BeforeEach
    fun setUp() {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "policy-service"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:0000"

        val topology = newPolicySaga.createTopology(props)

        topologyTestDriver = TopologyTestDriver(topology, props)
    }

    @AfterEach
    fun clean() = try {
        topologyTestDriver.close()
    } catch (e: Exception) {
    }

    @Test
    fun whenAllEventsAndNoTimeoutThenSagaCompleted() {
        val proposalAcceptedEvent = Event at 10
        val proposalAcceptedRecord = proposalAcceptedEvent.create(
                PROPOSAL_EVENTS_TOPIC,
                PROPOSAL_ID,
                pack(PROPOSAL_ID, 1, ProposalAcceptedEvent(PROPOSAL_ID, "OC", 3))
        )
        topologyTestDriver.pipeInput(proposalAcceptedRecord)

        val eventAt5sec = Event at 30
        val premiumRecord = eventAt5sec.create(
                PREMIUM_EVENTS_TOPIC,
                PROPOSAL_ID,
                pack(PROPOSAL_ID, 1, PremiumCalculatedEvent(PROPOSAL_ID, BigDecimal.valueOf(20)))

        )
        topologyTestDriver.pipeInput(premiumRecord)

        val firstRead: InsuranceCreationSaga = read(
                POLICY_EVENTS_TOPIC
        )!!
        val secondRead: InsuranceCreationSaga? = read(
                POLICY_EVENTS_TOPIC
        )
        assertNotNull(firstRead)
        assertEquals(firstRead.premiumCalculatedEvent.amount, BigDecimal("20.0"))
        assertNull(secondRead)
     }

    private inline fun <reified T>read(topicName: String): T? {
        val read = topologyTestDriver.readOutput(
                POLICY_EVENTS_TOPIC,
                StringDeserializer(),
                EventEnvelopeSerde().deserializer()
        ) ?: return null

        return read.value().unpack(T::class.java)
    }
}



private object Event {
    infix fun at(time: Long): ConsumerRecordFactory<String, EventEnvelope> {
        val record =  ConsumerRecordFactory<String, EventEnvelope>(
                StringSerializer(),
                EventEnvelopeSerde().serializer(),
                Duration.ofSeconds(time).toMillis()
        )

        return record
    }
}