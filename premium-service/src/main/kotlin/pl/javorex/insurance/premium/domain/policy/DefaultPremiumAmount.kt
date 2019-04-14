package pl.javorex.insurance.premium.domain.policy

import pl.javorex.insurance.premium.domain.vo.NumberOfPremium
import pl.javorex.insurance.premium.domain.vo.PremiumAmount
import java.math.BigDecimal
import java.math.RoundingMode

internal object DefaultPremiumAmount : PremiumCalculationPolicy {
    private val overallPremium: BigDecimal = BigDecimal.TEN
    override fun applyTo(numberOfPremium: NumberOfPremium): PremiumAmount {
        val value = overallPremium.divide(
                BigDecimal.valueOf(numberOfPremium.value.toLong()),
                RoundingMode.HALF_UP
        )
        return PremiumAmount(value)
    }
}