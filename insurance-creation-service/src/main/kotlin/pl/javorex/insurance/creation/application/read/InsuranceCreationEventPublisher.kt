package pl.javorex.insurance.creation.application.read

import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

interface InsuranceCreationEventPublisher {
    fun ofErrorsForProposal(proposalId: String) : Publisher<String>
    fun ofInsuranceCreatedForProposal(proposalId: String): Flux<String>
}