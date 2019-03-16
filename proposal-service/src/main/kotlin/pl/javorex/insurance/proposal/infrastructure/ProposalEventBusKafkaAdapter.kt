package pl.javorex.insurance.proposal.infrastructure

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import pl.javorex.insurance.proposal.application.ProposalEventBus
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.kafka.streams.event.EventEnvelopeSerde
import java.util.*
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.pack
import java.time.Instant

private val NO_PARTITION = null
@Service
class ProposalEventBusKafkaAdapter(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalEventsTopic: String
) : ProposalEventBus {
    private val producer = ProducerFactory.createProducer(bootstrapServers)

    override fun emit(proposalAccepted: ProposalAcceptedEvent, version: Long) {
        val timestamp = Instant.now().toEpochMilli()
        val key = proposalAccepted.proposalId
        val value = pack(proposalAccepted.proposalId, version, proposalAccepted)
                .withTimestamp(timestamp)

        producer.send(
                ProducerRecord(proposalEventsTopic, NO_PARTITION, timestamp, key, value)
        )
    }
}

private object ProducerFactory {
    fun createProducer(bootstrapServers: String): Producer<String, EventEnvelope> {
        val props = Properties()
        props[BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[CLIENT_ID_CONFIG] = "ProposalEventsProducer"
        props[KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java!!.getName()
        props[VALUE_SERIALIZER_CLASS_CONFIG] = EventEnvelopeSerde().serializer()::class.java!!.getName()

        return KafkaProducer(props)
    }
}