package pl.javorex.insurance.premium.application

import pl.javorex.insurance.creation.domain.event.CreateInsurance
import pl.javorex.insurance.premium.domain.DefaultPremiumAmount
import pl.javorex.insurance.premium.domain.NumberOfPremium
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.insurance.premium.domain.event.PremiumEvent
import pl.javorex.util.function.Failure
import pl.javorex.util.function.Success
import pl.javorex.util.function.Try

class ProposalAcceptedListener (
        private val premiumEventBus: PremiumEventBus
) {
    fun onProposalAccepted(createInsurance: CreateInsurance, insuranceVersion: Long) {
        val calculationTry = Try {
            val numberOfPremium = NumberOfPremium(createInsurance.numberOfPremiums)
            DefaultPremiumAmount.applyTo(numberOfPremium)
        }

        val event: PremiumEvent = when (calculationTry) {
            is Success -> PremiumCalculatedEvent(calculationTry.success.value)
            is Failure -> PremiumCalculationFailedEvent(calculationTry.error)
        }
        premiumEventBus.emit(event, createInsurance.insuranceId, insuranceVersion)
    }
}