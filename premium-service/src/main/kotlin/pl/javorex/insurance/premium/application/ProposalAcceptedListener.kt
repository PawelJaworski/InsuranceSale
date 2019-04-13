package pl.javorex.insurance.premium.application

import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.premium.domain.DefaultPremiumAmount
import pl.javorex.insurance.premium.domain.NumberOfPremium
import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailed
import pl.javorex.insurance.premium.domain.event.PremiumEvent
import pl.javorex.util.function.Failure
import pl.javorex.util.function.Success
import pl.javorex.util.function.Try

class ProposalAcceptedListener (
        private val premiumEventBus: PremiumEventBus
) {
    fun onProposalAccepted(insuranceCreationStarted: InsuranceCreationStarted, insuranceVersion: Long) {
        val calculationTry = Try {
            val numberOfPremium = NumberOfPremium(insuranceCreationStarted.numberOfPremiums)
            DefaultPremiumAmount.applyTo(numberOfPremium)
        }

        val event: PremiumEvent = when (calculationTry) {
            is Success -> PremiumCalculationCompleted(calculationTry.success.value)
            is Failure -> PremiumCalculationFailed(calculationTry.error)
        }
        premiumEventBus.emit(event, insuranceCreationStarted.insuranceId, insuranceVersion)
    }
}