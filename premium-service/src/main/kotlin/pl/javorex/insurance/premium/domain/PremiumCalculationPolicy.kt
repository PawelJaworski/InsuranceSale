package pl.javorex.insurance.premium.domain

internal interface PremiumCalculationPolicy {
    fun `for`(numberOfPremium: NumberOfPremium): PremiumAmount
}