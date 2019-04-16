package pl.javorex.insurance.creation.application

import pl.javorex.event.util.EventEnvelope
import pl.javorex.event.util.unambiguousVersionKeyOf
import pl.javorex.insurance.creation.domain.event.InsuranceCreationStarted
import pl.javorex.insurance.proposal.event.ProposalAccepted
import pl.javorex.kafka.streams.event.EventUniquenessListener
import pl.javorex.kafka.streams.event.ProcessorEventBus
import kotlin.random.Random

object UniqueProposalAcceptedEventVersionListener : EventUniquenessListener {
    override fun onUniqueViolated(error: EventEnvelope, eventBus: ProcessorEventBus) {
        eventBus.emitError(error.aggregateId, error.aggregateVersion, "Proposal version outdated.")
    }

    override fun onFirst(proposalAcceptedEvent: EventEnvelope, eventBus: ProcessorEventBus) {
       check(proposalAcceptedEvent.isTypeOf(ProposalAccepted::class.java)) {
           "Event should be type of ${ProposalAccepted::class.java} in $proposalAcceptedEvent"
       }

        val proposalAccepted = proposalAcceptedEvent.unpack(ProposalAccepted::class.java)
        val insuranceId = generateInsuranceId()
        val insuranceCreationStarted = InsuranceCreationStarted(
                unambiguousVersionKeyOf(proposalAcceptedEvent),
                insuranceId,
                proposalAccepted.insuranceProduct,
                proposalAccepted.numberOfPremiums)
        eventBus.emit(insuranceId, 1, insuranceCreationStarted)
    }

    private fun generateInsuranceId() = "insurance-${Random(System.currentTimeMillis()).nextInt(1, Int.MAX_VALUE)}"
}