package pl.javorex.insurance.premium.domain.event

data class PremiumCalculationFailedEvent(val error: String = "") : PremiumEvent