package pl.javorex.insurance.creation.adapter

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.connect.json.JsonDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.event.util.EventEnvelope
import pl.javorex.insurance.creation.application.read.InsuranceCreationEventsReading
import pl.javorex.kafka.streams.event.EventEnvelopeDeserializer
import pl.javorex.kafka.streams.event.EventEnvelopeSerde
import pl.javorex.util.kafka.common.serialization.JsonPOJODeserializer
import reactor.core.publisher.ConnectableFlux
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.util.HashMap

@Service
class InsuranceCreationEventsReadingImpl(
        @Value("\${kafka.topic.insurance-creation-error-events}") val topic: String,
        @Value("\${kafka.bootstrap-servers}") val bootstrapServers: String,
        @Value("\${kafka.consumer.groupId.policyEventsRead}}") val groupId: String,
        @Value("\${kafka.consumer.clientId.policyEventsRead}") val clientId: String
) : InsuranceCreationEventsReading {
    private val flux: ConnectableFlux<ReceiverRecord<String, EventEnvelope>> = KafkaReceiver
            .create<String, EventEnvelope>(
                    receiverOptions(listOf(topic))
            )
            .receive()
            .replay(0)

    override fun forProposalId(proposalId: String) : Flux<String> = flux
            .autoConnect()
            .filter{ it.key() == proposalId }
            .filter{ it.value().isTypeOf(String::class.java)}
            .map { it.value().unpack(String::class.java) }

    private fun receiverOptions(topics: Collection<String>): ReceiverOptions<String, EventEnvelope> {
        return receiverOptions()
                .addAssignListener { println("Group $groupId partitions assigned $it") }
                .addRevokeListener { println("Group $groupId partitions revoked $it") }
                .subscription(topics)
    }

    private fun receiverOptions(): ReceiverOptions<String, EventEnvelope> {
        val props = HashMap<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        props[ConsumerConfig.CLIENT_ID_CONFIG] = clientId
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = EventEnvelopeDeserializer::class.java

        return ReceiverOptions.create<String, EventEnvelope>(props)
    }
}