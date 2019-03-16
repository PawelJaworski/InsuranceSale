package pl.javorex.insurance.premium.domain.event

data class InsuredRegistrationFailed(val error: String) : InsuredEvent