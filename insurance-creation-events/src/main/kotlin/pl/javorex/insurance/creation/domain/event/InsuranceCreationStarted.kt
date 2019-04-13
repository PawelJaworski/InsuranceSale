package pl.javorex.insurance.creation.domain.event

data class InsuranceCreationStarted(
        val insuranceId: String = "",
        val insuranceProduct: String = "",
        val numberOfPremiums: Int = 0
)