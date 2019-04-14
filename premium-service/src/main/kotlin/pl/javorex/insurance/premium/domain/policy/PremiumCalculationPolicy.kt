package pl.javorex.insurance.premium.domain.policy

import pl.javorex.insurance.premium.domain.vo.NumberOfPremium
import pl.javorex.insurance.premium.domain.vo.PremiumAmount

internal interface PremiumCalculationPolicy {
    fun applyTo(numberOfPremium: NumberOfPremium): PremiumAmount
}