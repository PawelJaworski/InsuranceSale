package pl.javorex.insurance.premium.application

import pl.javorex.insurance.premium.domain.DefaultPremiumAmount
import pl.javorex.insurance.premium.domain.NumberOfPremium
import pl.javorex.insurance.premium.domain.premiumAmount
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.insurance.premium.domain.event.PremiumEvent
import pl.javorex.util.function.Failure
import pl.javorex.util.function.Success
import pl.javorex.util.function.Try
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent

class ProposalAcceptedListener (
        private val premiumEventBus: PremiumEventBus
) {
    fun onProposalAccepted(proposalAccepted: ProposalAcceptedEvent, proposalVersion: Long) {
        val calculationTry = Try {
            val numberOfPremium = NumberOfPremium(proposalAccepted.numberOfPremiums)
            premiumAmount() FOR numberOfPremium USING DefaultPremiumAmount
        }

        val event: PremiumEvent = when (calculationTry) {
            is Success -> PremiumCalculatedEvent(calculationTry.success.value)
            is Failure -> PremiumCalculationFailedEvent(calculationTry.error)
        }
        premiumEventBus.emit(event, proposalAccepted.proposalId, proposalVersion)
    }
}