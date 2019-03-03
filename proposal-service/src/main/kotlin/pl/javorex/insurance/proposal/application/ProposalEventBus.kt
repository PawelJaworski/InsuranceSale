package pl.javorex.insurance.proposal.application

import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent

interface ProposalEventBus {
    fun emit(proposalAccepted: ProposalAcceptedEvent, version: Long)
}