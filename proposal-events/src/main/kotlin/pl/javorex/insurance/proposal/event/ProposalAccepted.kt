package pl.javorex.insurance.proposal.event

data class ProposalAccepted(
        val proposalId: String,
        val insuranceProduct: String,
        val numberOfPremiums: Int
)