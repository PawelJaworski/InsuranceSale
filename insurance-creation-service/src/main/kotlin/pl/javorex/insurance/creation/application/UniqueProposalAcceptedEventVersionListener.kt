package pl.javorex.insurance.creation.application

import pl.javorex.event.util.EventEnvelope
import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.proposal.event.ProposalAccepted
import pl.javorex.kafka.streams.event.EventUniquenessListener
import pl.javorex.kafka.streams.event.ProcessorEventBus

object UniqueProposalAcceptedEventVersionListener : EventUniquenessListener {
    override fun onUniqueViolated(error: EventEnvelope, eventBus: ProcessorEventBus) {
        eventBus.emitError(error.aggregateId, error.aggregateVersion, "Proposal version outdated.")
    }

    override fun onFirst(event: EventEnvelope, eventBus: ProcessorEventBus) {
       check(event.isTypeOf(ProposalAccepted::class.java)) {
           "Event should be type of ${ProposalAccepted::class.java} in $event"
       }

        val proposalAcceptedEvent = event.unpack(ProposalAccepted::class.java)
        val insuranceCreationStarted = InsuranceCreationStarted(
                proposalAcceptedEvent.proposalId,
                proposalAcceptedEvent.insuranceProduct,
                proposalAcceptedEvent.numberOfPremiums)
        eventBus.emit(event.aggregateId, event.aggregateVersion, insuranceCreationStarted)
    }
}