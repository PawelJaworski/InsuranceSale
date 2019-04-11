package pl.javorex.insurance.creation.application

import pl.javorex.event.util.EventEnvelope
import pl.javorex.insurance.creation.domain.event.CreateInsurance
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.kafka.streams.event.EventUniquenessListener
import pl.javorex.kafka.streams.event.ProcessorEventBus

object UniqueProposalAcceptedEventVersionListener : EventUniquenessListener {
    override fun onUniqueViolated(error: EventEnvelope, eventBus: ProcessorEventBus) {
        eventBus.emitError(error.aggregateId, error.aggregateVersion, "Proposal version outdated.")
    }

    override fun onFirst(event: EventEnvelope, eventBus: ProcessorEventBus) {
       check(event.isTypeOf(ProposalAcceptedEvent::class.java)) {
           "Event should be type of ${ProposalAcceptedEvent::class.java} in $event"
       }

        val proposalAcceptedEvent = event.unpack(ProposalAcceptedEvent::class.java)
        val createInsuranceFromProposalEvent = CreateInsurance(
                proposalAcceptedEvent.proposalId,
                proposalAcceptedEvent.insuranceProduct,
                proposalAcceptedEvent.numberOfPremiums)
        eventBus.emit(event.aggregateId, event.aggregateVersion, createInsuranceFromProposalEvent)
    }
}