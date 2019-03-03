package pl.javorex.insurance.premium.application

import pl.javorex.insurance.premium.domain.CalculateDefaultPremiumAmount
import pl.javorex.insurance.premium.domain.NumberOfPremium
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent

class ProposalAcceptedListener (
        private val premiumEventBus: PremiumEventBus
) {
    fun onProposalAccepted(proposalAccepted: ProposalAcceptedEvent, proposalVersion: Long) {
        val premiumAmount = CalculateDefaultPremiumAmount.`for`(
                NumberOfPremium(proposalAccepted.numberOfPremiums)
        )

        val event = PremiumCalculatedEvent(premiumAmount.value)
        premiumEventBus.emit(event, proposalAccepted.proposalId, proposalVersion)
    }
}