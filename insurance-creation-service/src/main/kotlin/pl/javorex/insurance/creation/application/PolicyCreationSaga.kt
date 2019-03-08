package pl.javorex.insurance.creation.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumCalculationFailedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.event.EventEnvelope

data class InsuranceCreationSagaCompleted(
        val version: Long,
        val proposalAcceptedEvent: ProposalAcceptedEvent,
        val premiumCalculatedEvent: PremiumCalculatedEvent
)

data class InsuranceCreationSagaCorrupted(
        val version: Long,
        val error: String
)

class InsuranceCreationSagaBuilder {
    var version: Long? = null
    var proposalAcceptedEvent: ProposalAcceptedEvent? = null
    var premiumCalculatedEvent: PremiumCalculatedEvent? = null
    var errors = hashMapOf<Long, String>()

    fun mergeEvent(event: EventEnvelope): InsuranceCreationSagaBuilder {
        when {
            version != null && version!! < event.aggregateVersion -> {
                errors[version!!] = "request.outdated"
                reset()
            }
        }
        if (version != null && version!! < event.aggregateVersion) {

        }
        version = event.aggregateVersion
        when {
            event.isTypeOf(PremiumCalculationFailedEvent::class.java) ->
                errors[version!!] = event.unpack(PremiumCalculationFailedEvent::class.java).error
            event.isTypeOf(ProposalAcceptedEvent::class.java) && proposalAcceptedEvent != null ->
                errors[version!!] = "error.double.proposal.accepted"
            event.isTypeOf(ProposalAcceptedEvent::class.java) ->
                proposalAcceptedEvent = event.unpack(ProposalAcceptedEvent::class.java)
            event.isTypeOf(PremiumCalculatedEvent::class.java) && premiumCalculatedEvent != null ->
                errors[version!!] = "error.double.premium.calculated"
            event.isTypeOf(PremiumCalculatedEvent::class.java) ->
                premiumCalculatedEvent = event.unpack(PremiumCalculatedEvent::class.java)
        }

        return this
    }

    fun buildCorrupted(): List<InsuranceCreationSagaCorrupted> {
        return errors.keys.sorted()
                .map { InsuranceCreationSagaCorrupted(it, errors[it]!!) }
    }


    fun buildCompleted() =
            InsuranceCreationSagaCompleted(version!!, proposalAcceptedEvent!!, premiumCalculatedEvent!!)

    private fun reset() {
        this.proposalAcceptedEvent = null
        this.premiumCalculatedEvent = null
    }

    fun isComplete() = premiumCalculatedEvent != null && proposalAcceptedEvent != null

    private fun isCorrupted(version: Long) = errors.contains(version)
}