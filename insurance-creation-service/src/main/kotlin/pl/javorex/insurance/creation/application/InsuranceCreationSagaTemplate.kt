package pl.javorex.insurance.creation.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailed
import pl.javorex.event.util.EventSagaBuilder
import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.premium.domain.event.InsuredRegistrationCompleted
import java.time.Duration.ofSeconds

internal fun newSagaTemplate() =
        EventSagaBuilder()
                .withTimeout(ofSeconds(5))
                .startsWith(InsuranceCreationStarted::class.java)
                .requires(PremiumCalculationCompleted::class.java)
                //.requires(InsuredRegistrationCompleted::class.java)
                .expectErrors(PremiumCalculationFailed::class.java)
                .build()


