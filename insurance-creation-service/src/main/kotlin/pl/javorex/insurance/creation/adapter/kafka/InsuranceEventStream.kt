package pl.javorex.insurance.creation.adapter.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import pl.javorex.event.util.EventEnvelope
import pl.javorex.insurance.creation.application.read.InsuranceCreationEventPublisher
import pl.javorex.insurance.creation.domain.event.InsuranceCreated
import pl.javorex.kafka.streams.event.EventEnvelopeDeserializer
import reactor.core.publisher.ConnectableFlux
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import java.util.HashMap

class InsuranceEventStream(
        private val bootstrapServers: String,
        insuranceTopic: String,
        insuranceErrorTopic: String
) : InsuranceCreationEventPublisher {
    private val messageFlux: ConnectableFlux<ReceiverRecord<String, EventEnvelope>> = KafkaReceiver
        .create<String, EventEnvelope>(
            subscriptionOf(listOf(insuranceTopic))
        )
        .receive()
        .replay(0)

    private val errorFlux: ConnectableFlux<ReceiverRecord<String, EventEnvelope>> = KafkaReceiver
            .create<String, EventEnvelope>(
                    subscriptionOf(listOf(insuranceErrorTopic))
            )
            .receive()
            .replay(0)

    override fun ofErrorsForProposal(proposalId: String) : Flux<String> = errorFlux
            .autoConnect()
            .filter{ it.key() == proposalId }
            .filter{ it.value().isTypeOf(String::class.java)}
            .map { it.value().unpack(String::class.java) }

    override fun ofInsuranceCreatedForProposal(proposalId: String) : Flux<String>  = messageFlux
        .autoConnect()
        .filter{ it.value().isTypeOf(InsuranceCreated::class.java)}
        .map { it.value().unpack(InsuranceCreated::class.java) }
        .filter{ it.source.aggregateId == proposalId }
        .map { "Insurance ${it.insuranceNumber} created."}

    private fun subscriptionOf(topics: Collection<String>): ReceiverOptions<String, EventEnvelope> {
        return subscriptionOf()
                .subscription(topics)
    }

    private fun subscriptionOf(): ReceiverOptions<String, EventEnvelope> {
        val props = HashMap<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "policy-events-read-groupId"
        props[ConsumerConfig.CLIENT_ID_CONFIG] = "policy-events-read-clientId"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = EventEnvelopeDeserializer::class.java

        return ReceiverOptions.create<String, EventEnvelope>(props)
    }
}