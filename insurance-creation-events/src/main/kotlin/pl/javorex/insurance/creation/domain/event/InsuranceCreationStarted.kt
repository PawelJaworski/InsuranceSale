package pl.javorex.insurance.creation.domain.event

import pl.javorex.event.util.NON_EXISTENT
import pl.javorex.event.util.UnambiguousEventVersion
import pl.javorex.event.util.UnambiguousVersionKey

data class InsuranceCreationStarted(
        val source: UnambiguousVersionKey = NON_EXISTENT,
        val insuranceId: String = "",
        val insuranceProduct: String = "",
        val numberOfPremiums: Int = 0
)