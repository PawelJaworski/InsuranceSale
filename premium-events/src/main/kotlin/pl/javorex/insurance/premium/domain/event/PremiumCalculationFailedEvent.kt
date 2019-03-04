package pl.javorex.insurance.premium.domain.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PremiumCalculationFailedEvent
@JsonCreator
constructor(@JsonProperty("error") val error: String) : PremiumEvent