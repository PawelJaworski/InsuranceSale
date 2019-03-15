package pl.javorex.insurance.premium.domain.event

import java.math.BigDecimal

data class PremiumCalculatedEvent(val amount: BigDecimal = BigDecimal.ZERO) : PremiumEvent