package pl.javorex.insurance.proposal.rest.command

data class AcceptProposalCommand(
        val proposalId: String,
        val version: Long,
        val insuranceProduct: String,
        val numberOfPremiums: Int
)