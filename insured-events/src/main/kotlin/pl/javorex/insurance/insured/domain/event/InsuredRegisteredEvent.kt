package pl.javorex.insurance.premium.domain.event

import java.math.BigDecimal

data class InsuredRegisteredEvent(val amount: BigDecimal = BigDecimal.ZERO) : InsuredEvent