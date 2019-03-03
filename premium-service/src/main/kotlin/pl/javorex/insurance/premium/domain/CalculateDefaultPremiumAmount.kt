package pl.javorex.insurance.premium.domain

import java.math.BigDecimal
import java.math.RoundingMode

object CalculateDefaultPremiumAmount : PremiumPolicy {
    private val overallPremium: BigDecimal = BigDecimal.TEN
    fun `for`(numberOfPremium: NumberOfPremium): PremiumAmount {
        val value = overallPremium.divide(
                BigDecimal.valueOf(numberOfPremium.value.toLong()),
                RoundingMode.HALF_UP
        )
        return PremiumAmount(value)
    }
}