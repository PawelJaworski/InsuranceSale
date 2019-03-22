package pl.javorex.insurance.creation.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.EventSagaBuilder
import java.time.Duration
import java.time.Duration.ofSeconds

internal object InsuranceCreationSagaTemplateFactory {
    fun newSagaTemplate() =
            EventSagaBuilder()
                    .withTimeout(ofSeconds(5))
                    .startsWith(ProposalAcceptedEvent::class.java)
                    .requires(PremiumCalculatedEvent::class.java)
                    .expectErrors(PremiumCalculationFailedEvent::class.java)
                    .build()
}

