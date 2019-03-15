package pl.javorex.insurance.creation.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.event.util.EventEnvelope

data class InsuranceCreationSagaCompleted(
        val proposalAcceptedEvent: ProposalAcceptedEvent,
        val premiumCalculatedEvent: PremiumCalculatedEvent
)

data class InsuranceCreationSagaCorrupted(
        val version: Long,
        val error: String
)