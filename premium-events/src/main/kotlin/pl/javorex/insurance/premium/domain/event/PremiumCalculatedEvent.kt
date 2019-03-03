package pl.javorex.insurance.premium.domain.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class PremiumCalculatedEvent
@JsonCreator
constructor(
        @JsonProperty("amount") val amount: BigDecimal
)