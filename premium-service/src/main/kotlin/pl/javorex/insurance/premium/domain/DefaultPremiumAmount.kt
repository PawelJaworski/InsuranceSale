package pl.javorex.insurance.premium.domain

import java.math.BigDecimal
import java.math.RoundingMode

internal object DefaultPremiumAmount : PremiumCalculationPolicy {
    private val overallPremium: BigDecimal = BigDecimal.TEN
    override fun `for`(numberOfPremium: NumberOfPremium): PremiumAmount {
        val value = overallPremium.divide(
                BigDecimal.valueOf(numberOfPremium.value.toLong()),
                RoundingMode.HALF_UP
        )
        return PremiumAmount(value)
    }
}