package pl.javorex.insurance.premium.application

import org.springframework.stereotype.Service
import pl.javorex.insurance.premium.application.command.CalculatePremiumCommand
import pl.javorex.insurance.premium.domain.event.PremiumCalculatedEvent
import pl.javorex.util.event.EventEnvelope
import pl.javorex.util.event.pack
import java.math.BigDecimal

@Service
class CommandHandlers {
    val OVERALL_PREMIUM = BigDecimal.TEN
    fun handle(calculatePremium: CalculatePremiumCommand): PremiumCalculatedEvent {
        return PremiumCalculatedEvent(
            OVERALL_PREMIUM.divide(
                    BigDecimal.valueOf(calculatePremium.numberOfPremiums.toLong())
            )
        )
    }
}