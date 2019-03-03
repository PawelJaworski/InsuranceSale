package pl.javorex.insurance.premium.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import pl.javorex.insurance.premium.application.ProposalAcceptedListener

@Configuration
class ServiceConfig(
        val premiumEventBus: PremiumEventBusKafkaAdapter
) {
    @Bean
    fun proposalAcceptedListener() : ProposalAcceptedListener {
        return ProposalAcceptedListener(premiumEventBus)
    }
}