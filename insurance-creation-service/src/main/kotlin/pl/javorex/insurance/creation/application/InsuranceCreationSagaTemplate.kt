package pl.javorex.insurance.creation.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.event.util.EventSagaBuilder
import pl.javorex.insurance.creation.domain.event.CreateInsurance
import java.time.Duration.ofSeconds

internal object InsuranceCreationSagaTemplateFactory {
    fun newSagaTemplate() =
            EventSagaBuilder()
                    .withTimeout(ofSeconds(5))
                    .startsWith(CreateInsurance::class.java)
                    .requires(PremiumCalculatedEvent::class.java)
                    .expectErrors(PremiumCalculationFailedEvent::class.java)
                    .build()
}

