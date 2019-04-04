package pl.javorex.insurance.creation.domain.event

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent

data class InsuranceCreationSagaCompleted(
        val proposalAcceptedEvent: CreateInsuranceFromProposal,
        val premiumCalculatedEvent: PremiumCalculatedEvent
)