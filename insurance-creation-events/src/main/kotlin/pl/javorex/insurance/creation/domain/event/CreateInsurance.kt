package pl.javorex.insurance.creation.domain.event

data class CreateInsurance(
        val insuranceId: String = "",
        val insuranceProduct: String = "",
        val numberOfPremiums: Int = 0
)