package pl.javorex.insurance.creation.application.read

import org.reactivestreams.Publisher

interface InsuranceCreationEventsReading {
    fun forProposalId(proposalId: String) : Publisher<String>
}