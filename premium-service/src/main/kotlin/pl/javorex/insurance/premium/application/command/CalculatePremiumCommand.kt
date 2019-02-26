package pl.javorex.insurance.premium.application.command

data class CalculatePremiumCommand(
        val aggregateId: String,
        val aggregateVersion: Long,
        val product: String,
        val numberOfPremiums: Int
)