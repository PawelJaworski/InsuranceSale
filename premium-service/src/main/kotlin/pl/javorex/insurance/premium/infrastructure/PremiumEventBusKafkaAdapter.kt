package pl.javorex.insurance.premium.infrastructure

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.javorex.insurance.premium.application.PremiumEventBus
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumEvent
import pl.javorex.util.event.EventEnvelope
import pl.javorex.util.event.pack
import pl.javorex.util.kafka.streams.event.EventEnvelopeSerde
import java.util.*

@Service
class PremiumEventBusKafkaAdapter(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.premium-events}") private val premiumEventsTopic: String
) : PremiumEventBus {
    override fun emit(
            premiumCalculatedEvent: PremiumEvent,
            aggregateId: String,
            aggregateVersion: Long
    ) {
        val record = ProducerRecord<String, EventEnvelope>(
                premiumEventsTopic,
                pack(aggregateId, aggregateVersion, premiumCalculatedEvent)
        )
        producer.send(record)
    }

    private val producer = ProducerFactory.createProducer(bootstrapServers)

}

private object ProducerFactory {
    fun createProducer(bootstrapServers: String): Producer<String, EventEnvelope> {
        val props = Properties()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.CLIENT_ID_CONFIG] = "PremiumEventsProducer"
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java!!.getName()
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = EventEnvelopeSerde().serializer()::class.java!!.getName()

        return KafkaProducer(props)
    }
}