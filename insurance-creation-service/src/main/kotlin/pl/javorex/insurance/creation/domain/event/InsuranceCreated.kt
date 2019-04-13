package pl.javorex.insurance.creation.domain.event

import pl.javorex.insurance.premium.domain.event.PremiumCalculationCompleted

data class InsuranceCreated(
        val creationCriteria: InsuranceCreationStarted,
        val premiumCalculationCompleted: PremiumCalculationCompleted
)