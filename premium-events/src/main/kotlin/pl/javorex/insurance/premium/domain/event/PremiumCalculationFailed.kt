package pl.javorex.insurance.premium.domain.event

data class PremiumCalculationFailed(val error: String = "") : PremiumEvent