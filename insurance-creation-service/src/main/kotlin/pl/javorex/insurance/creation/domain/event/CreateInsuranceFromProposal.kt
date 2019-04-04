package pl.javorex.insurance.creation.domain.event

class CreateInsuranceFromProposal(
        val proposalId: String,
        val insuranceProduct: String,
        val numberOfPremiums: Int
)