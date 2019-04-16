package pl.javorex.insurance.creation.adapter

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.javorex.insurance.creation.application.InsuranceCreationSagaEventListener
import pl.javorex.insurance.creation.application.InsuranceFactory

@Configuration
internal class SpringContextConfig {
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