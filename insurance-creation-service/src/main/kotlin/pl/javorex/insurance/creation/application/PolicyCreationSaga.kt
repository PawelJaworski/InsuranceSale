package pl.javorex.insurance.creation.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.util.event.EventEnvelope

data class InsuranceCreationSaga(
        val proposalAcceptedEvent: ProposalAcceptedEvent,
        val premiumCalculatedEvent: PremiumCalculatedEvent
)

class Builder {
    var proposalAcceptedEvent: ProposalAcceptedEvent? = null
    var premiumCalculatedEvent: PremiumCalculatedEvent? = null
    var errors: Array<String> = arrayOf()

    fun mergeEvent(event: EventEnvelope): Builder {
        when {
            event.isTypeOf(ProposalAcceptedEvent::class.java) && proposalAcceptedEvent != null ->
                errors += "error.double.proposal.accepted"
            event.isTypeOf(ProposalAcceptedEvent::class.java) ->
                proposalAcceptedEvent = event.unpack(ProposalAcceptedEvent::class.java)
            event.isTypeOf(PremiumCalculatedEvent::class.java) && premiumCalculatedEvent != null ->
                errors += "error.double.premium.calculated"
            event.isTypeOf(PremiumCalculatedEvent::class.java) ->
                premiumCalculatedEvent = event.unpack(PremiumCalculatedEvent::class.java)
        }

        return this
    }

    fun isComplete() = premiumCalculatedEvent != null && proposalAcceptedEvent != null

    fun getMissed(): String = when {
        proposalAcceptedEvent == null -> "error.proposal.accepted.missed"
        premiumCalculatedEvent == null -> "error.premium.calculated.missed"
        else -> {
            "ok"
        }
    }

    fun build(): InsuranceCreationSaga {
        if (isCorrupted()) {
            throw IllegalStateException("Saga is corrupted")
        }
        return InsuranceCreationSaga(
                proposalAcceptedEvent!!,
                premiumCalculatedEvent!!
        )
    }

    fun isCorrupted(): Boolean {
        return !errors.isEmpty()
                || proposalAcceptedEvent == null
                || premiumCalculatedEvent == null
    }
}