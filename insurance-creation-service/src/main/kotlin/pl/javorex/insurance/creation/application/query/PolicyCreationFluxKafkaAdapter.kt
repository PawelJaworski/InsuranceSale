package pl.javorex.insurance.creation.application.query

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.creation.query.PolicyCreation
import reactor.core.publisher.ConnectableFlux
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.util.HashMap

@Service
class PolicyCreationFluxKafkaAdapter(
        @Value("\${kafka.topic.policy-events}") val topic: String,
        @Value("\${kafka.bootstrap-servers}") val bootstrapServers: String,
        @Value("\${kafka.consumer.groupId.policyEventsRead}}") val groupId: String,
        @Value("\${kafka.consumer.clientId.policyEventsRead}") val clientId: String
) : PolicyCreation {
    private val flux: ConnectableFlux<ReceiverRecord<String, String>> = KafkaReceiver
            .create<String, String>(
                    receiverOptions(listOf(topic))
            )
            .receive()
            .replay(0)

    override fun fluxForProposalId(proposalId: String) : Flux<String> = flux
            .autoConnect()
            .filter{ it.key() == proposalId }
            .map { it.value() }

    private fun receiverOptions(topics: Collection<String>): ReceiverOptions<String, String> {
        return receiverOptions()
                .addAssignListener { println("Group $groupId partitions assigned $it") }
                .addRevokeListener { println("Group $groupId partitions revoked $it") }
                .subscription(topics)
    }

    private fun receiverOptions(): ReceiverOptions<String, String> {
        val props = HashMap<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        props[ConsumerConfig.CLIENT_ID_CONFIG] = clientId
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return ReceiverOptions.create<String, String>(props)
    }
}