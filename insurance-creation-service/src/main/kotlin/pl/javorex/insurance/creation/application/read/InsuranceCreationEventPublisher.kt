package pl.javorex.insurance.creation.application.read

import org.reactivestreams.Publisher

interface InsuranceCreationEventPublisher {
    fun forProposalId(proposalId: String) : Publisher<String>
}