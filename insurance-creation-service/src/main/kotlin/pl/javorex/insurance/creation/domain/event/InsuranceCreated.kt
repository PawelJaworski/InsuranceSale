package pl.javorex.insurance.creation.domain.event

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent

data class InsuranceCreated(
        val creationCriteria: CreateInsurance,
        val premiumCalculatedEvent: PremiumCalculatedEvent
)