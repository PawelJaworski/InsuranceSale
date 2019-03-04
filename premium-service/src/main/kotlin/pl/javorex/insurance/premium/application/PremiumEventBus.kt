package pl.javorex.insurance.premium.application

import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.insurance.premium.domain.event.PremiumEvent

interface PremiumEventBus {
    fun emit(
            event: PremiumEvent,
            aggregateId: String,
            version: Long
    )
}