package pl.javorex.insurance.creation.adapter

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.javorex.insurance.creation.adapter.kafka.InsuranceCreationKStream
import pl.javorex.insurance.creation.application.InsuranceCreationSagaEventListener
import pl.javorex.insurance.creation.application.InsuranceFactory

@Configuration
internal class SpringContextConfig(
        @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
        @Value("\${kafka.topic.proposal-events}") private val proposalTopic: String,
        @Value("\${kafka.topic.premium-events}") private val premiumTopic: String,
        @Value("\${kafka.topic.insurance-events}") private val insuranceTopic: String,
        @Value("\${kafka.topic.insurance-error-events}") private val insuranceErrorTopic: String
) {
    @Bean
    fun insuranceCreationKStream() =
            InsuranceCreationKStream(bootstrapServers, proposalTopic, premiumTopic, insuranceTopic, insuranceErrorTopic,
                    insuranceCreationSagaEventListener())

    @Bean
    fun insuranceCreationSagaEventListener() =
            InsuranceCreationSagaEventListener(insuranceFactory())

    @Bean
    fun insuranceFactory() =
            InsuranceFactory(insuranceNoGenerator())
    @Bean
    fun insuranceNoGenerator() =
            InsuranceNoGeneratorTimestampImpl()
}