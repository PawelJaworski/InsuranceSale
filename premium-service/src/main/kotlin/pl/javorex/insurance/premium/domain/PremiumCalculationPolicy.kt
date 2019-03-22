package pl.javorex.insurance.premium.domain

internal interface PremiumCalculationPolicy {
    fun applyTo(numberOfPremium: NumberOfPremium): PremiumAmount
}