package pl.javorex.insurance.premium.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import pl.javorex.insurance.premium.adapter.RollbackKStream
import pl.javorex.insurance.premium.application.ProposalAcceptedListener

@Configuration
class ServiceConfig(
        val premiumEventBus: PremiumEventBusKafkaAdapter,
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.premium-events}") private val premiumTopic: String,
        @Value("\${kafka.topic.insurance-creation-error-events}") private val insuranceErrorTopic: String
) {
    @Bean
    fun rollbackStreamListener() : RollbackKStream {
        return RollbackKStream(bootstrapServers, premiumTopic, insuranceErrorTopic, proposalAcceptedListener())
    }

    @Bean
    fun proposalAcceptedListener() : ProposalAcceptedListener {
        return ProposalAcceptedListener(premiumEventBus)
    }
}