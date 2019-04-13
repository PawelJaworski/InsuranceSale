package pl.javorex.insurance.creation.application

import junit.framework.Assert.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted
import pl.javorex.insurance.proposal.event.ProposalAccepted
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.pack
import pl.javorex.insurance.creation.adapter.InsuranceCreationSagaEventStream
import pl.javorex.insurance.creation.domain.event.InsuranceCreated
import pl.javorex.insurance.creation.domain.event.InsuranceCreationSagaCorrupted
import pl.javorex.kafka.streams.event.EventEnvelopeSerde
import java.math.BigDecimal
import java.time.Duration
import java.util.*

private const val PROPOSAL_ID = "proposal-1"
private const val BOOTSTRAP_SERVERS = "localhost:0000"
private const val PROPOSAL_EVENTS_TOPIC = "proposal-events-test"
private const val PREMIUM_EVENTS_TOPIC = "premium-events-test"
private const val POLICY_EVENTS_TOPIC = "policy-events-test"
private const val INSURANCE_CREATION_SAGA_TOPIC = "policy-saga-test"
private const val INSURANCE_CREATION_ERROR_TOPIC = "insurance-creation-error-test"

class PolicyCreationSagaStreamSpec {
    private lateinit var topologyTestDriver: TopologyTestDriver

    private val newPolicySaga = InsuranceCreationSagaEventStream(
            BOOTSTRAP_SERVERS,
            PROPOSAL_EVENTS_TOPIC,
            PREMIUM_EVENTS_TOPIC,
            POLICY_EVENTS_TOPIC,
            INSURANCE_CREATION_SAGA_TOPIC
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
        topologyTestDriver.pipe{
            aggregateRootId = PROPOSAL_ID
            topic = PROPOSAL_EVENTS_TOPIC
            event = ProposalAccepted(PROPOSAL_ID, "OC", 3)
            at = 0
        }

        topologyTestDriver.pipe {
            aggregateRootId = PROPOSAL_ID
            topic = PREMIUM_EVENTS_TOPIC
            event = PremiumCalculationCompleted(BigDecimal.valueOf(20))
            at = 10
        }

        val firstRead: InsuranceCreated? = read(
                POLICY_EVENTS_TOPIC
        )
        val secondRead: InsuranceCreated? = read(
                POLICY_EVENTS_TOPIC
        )
        assertNotNull(firstRead)
        assertEquals(firstRead!!.premiumCalculationCompleted.amount, BigDecimal("20.0"))
        assertNull(secondRead)
    }

    @Test
    fun whenAllEventsAndTimeoutThenSagaCorrupted() {
        topologyTestDriver.pipe{
            aggregateRootId = PROPOSAL_ID
            topic = PROPOSAL_EVENTS_TOPIC
            event = ProposalAccepted(PROPOSAL_ID, "OC", 3)
            at = 0
        }

        topologyTestDriver.pipe {
            aggregateRootId = PROPOSAL_ID
            topic = PREMIUM_EVENTS_TOPIC
            event = PremiumCalculationCompleted(BigDecimal.valueOf(20))
            at = 26
        }

        val completedSaga: InsuranceCreated? = read(
                POLICY_EVENTS_TOPIC
        )
        val corruptedSaga: InsuranceCreationSagaCorrupted? = read(
                INSURANCE_CREATION_ERROR_TOPIC
        )
        assertNull(completedSaga)
        assertNotNull(corruptedSaga)
        assertEquals(corruptedSaga!!.error, "error.timeout")
        assertNull(
            read(INSURANCE_CREATION_ERROR_TOPIC)
        )
    }
    private inline fun <reified T>read(topicName: String): T? {
        val read = topologyTestDriver.readOutput(
                topicName,
                StringDeserializer(),
                EventEnvelopeSerde().deserializer()
        ) ?: return null

        return read.value().unpack(T::class.java)
    }
}

fun TopologyTestDriver.pipe(buildRecord: ConsumerRecordBuilder.() -> Unit) {

    val builder = ConsumerRecordBuilder()
    builder.buildRecord()

    this.pipeInput(builder.build())

}

class ConsumerRecordBuilder {
    lateinit var aggregateRootId: String
    var version: Long = 0
    lateinit var topic: String
    var at: Long = 0
    lateinit var event: Any
    fun build(): ConsumerRecord<ByteArray, ByteArray> {
        val timestamp = Duration.ofSeconds(at).toMillis()
        val factory =  ConsumerRecordFactory<String, EventEnvelope>(
                StringSerializer(),
                EventEnvelopeSerde().serializer(),
                timestamp
        )
        return factory.create(
                topic,
                aggregateRootId,
                pack(aggregateRootId, version, event)
                        .withTimestamp(timestamp)
        )
    }
}
