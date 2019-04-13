package pl.javorex.insurance.proposal.application

import pl.javorex.insurance.proposal.event.ProposalAccepted

interface ProposalEventBus {
    fun emit(proposalAccepted: ProposalAccepted, version: Long)
}