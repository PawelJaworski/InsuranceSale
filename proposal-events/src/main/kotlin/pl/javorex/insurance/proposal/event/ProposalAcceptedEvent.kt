package pl.javorex.insurance.proposal.event

data class ProposalAcceptedEvent(
        val proposalId: String,
        val insuranceProduct: String,
        val numberOfPremiums: Int
)