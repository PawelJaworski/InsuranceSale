package pl.javorex.insurance.premium.adapter

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.javorex.insurance.premium.application.PremiumEventBus
import pl.javorex.insurance.premium.application.ProposalAcceptedListener

@Configuration
internal class SpringConfig(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.premium-events}") private val premiumTopic: String,
        @Value("\${kafka.topic.insurance-error-events}") private val insuranceErrorTopic: String
) {
    @Bean
    fun rollbackStreamListener() : PremiumCalculationRollbackKStream {
        return PremiumCalculationRollbackKStream(bootstrapServers, premiumTopic, insuranceErrorTopic, proposalAcceptedListener())
    }

    @Bean
    fun proposalAcceptedListener() : ProposalAcceptedListener {
        return ProposalAcceptedListener(premiumEventBus())
    }

    @Bean
    fun premiumEventBus() : PremiumEventBus {
        return PremiumEventBusKafkaImpl(bootstrapServers, premiumTopic)
    }
}