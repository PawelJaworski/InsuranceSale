package pl.javorex.insurance.creation.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
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

class Builder {
    var versions = mutableSetOf<Long>()
    var proposalAcceptedEvent = hashMapOf<Long, ProposalAcceptedEvent>()
    var premiumCalculatedEvent = hashMapOf<Long, PremiumCalculatedEvent>()
    var errors = hashMapOf<Long, String>()

    fun mergeEvent(event: EventEnvelope): Builder {
        val version = event.aggregateVersion
        versions.add(version)
        when {
            event.isTypeOf(ProposalAcceptedEvent::class.java) && proposalAcceptedEvent.contains(version) ->
                errors[version] = "error.double.proposal.accepted"
            event.isTypeOf(ProposalAcceptedEvent::class.java) ->
                proposalAcceptedEvent[version] = event.unpack(ProposalAcceptedEvent::class.java)
            event.isTypeOf(PremiumCalculatedEvent::class.java) && premiumCalculatedEvent.contains(version) ->
                errors[version] = "error.double.premium.calculated"
            event.isTypeOf(PremiumCalculatedEvent::class.java) ->
                premiumCalculatedEvent[version] = event.unpack(PremiumCalculatedEvent::class.java)
        }

        return this
    }

    fun isComplete(version: Long) = premiumCalculatedEvent.contains(version) && proposalAcceptedEvent.contains(version)

    fun isCorrupted(version: Long) = errors.contains(version)

    fun getMissed(): String = when {
        proposalAcceptedEvent == null -> "error.proposal.accepted.missed"
        premiumCalculatedEvent == null -> "error.premium.calculated.missed"
        else -> {
            "ok"
        }
    }

    fun buildMissing(): List<InsuranceCreationSagaCorrupted> {
        return versions.sorted()
                .filter { !isComplete(it) }
                .map { InsuranceCreationSagaCorrupted(it, "error.timeout") }
    }

    fun buildCorrupted(): List<InsuranceCreationSagaCorrupted> {
        return versions.sorted()
                .filter { isCorrupted(it) }
                .map { InsuranceCreationSagaCorrupted(it, errors[it]!!) }
    }


    fun build(): List<InsuranceCreationSagaCompleted> {
        return versions.sorted()
                .filter { isComplete(it) }
                .map { InsuranceCreationSagaCompleted(it, proposalAcceptedEvent[it]!!, premiumCalculatedEvent[it]!!) }
    }

    fun isCorrupted(): Boolean {
        return !errors.isEmpty()
                || proposalAcceptedEvent == null
                || premiumCalculatedEvent == null
    }
}