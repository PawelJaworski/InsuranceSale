package pl.javorex.insurance.premium.domain

import java.math.BigDecimal

data class PremiumAmount(val value: BigDecimal) : ValueObject