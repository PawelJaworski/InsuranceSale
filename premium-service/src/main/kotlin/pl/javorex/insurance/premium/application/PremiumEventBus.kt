package pl.javorex.insurance.premium.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent

interface PremiumEventBus {
    fun emit(
            proposalAccepted: PremiumCalculatedEvent,
            aggregateId: String,
            version: Long
    )
}