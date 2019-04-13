package pl.javorex.insurance.premium.domain.event

import java.math.BigDecimal

data class PremiumCalculationCompleted(val amount: BigDecimal = BigDecimal.ZERO) : PremiumEvent