package pl.javorex.insurance.creation.domain.event

import pl.javorex.event.util.NON_EXISTENT
import pl.javorex.event.util.UnambiguousVersionKey

data class InsuranceCreated(
        val source: UnambiguousVersionKey = NON_EXISTENT,
        val insuranceNumber: String = ""
)