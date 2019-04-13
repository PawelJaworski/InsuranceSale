package pl.javorex.insurance.premium.domain.event

import java.math.BigDecimal

data class InsuredRegistrationCompleted(val amount: BigDecimal = BigDecimal.ZERO) : InsuredEvent