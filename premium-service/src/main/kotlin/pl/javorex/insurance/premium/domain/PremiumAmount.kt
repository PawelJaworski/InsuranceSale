package pl.javorex.insurance.premium.domain

import java.math.BigDecimal

internal fun premiumAmount() = PremiumAmountBuilder()

internal data class PremiumAmount(val value: BigDecimal) : ValueObject

internal class PremiumAmountBuilder {
    lateinit var numberOfPremium: NumberOfPremium
    infix fun FOR(numberOfPremium: NumberOfPremium): PremiumAmountBuilder {
        this.numberOfPremium = numberOfPremium
        return this
    }
    infix fun USING(premiumCalculationPolicy: PremiumCalculationPolicy) =
            premiumCalculationPolicy.`for`(numberOfPremium)
}