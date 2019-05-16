package pl.javorex.insurance.creation.adapter

import org.reactivestreams.Publisher
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import pl.javorex.insurance.creation.application.read.InsuranceCreationEventPublisher


@RestController
class InsuranceCreationEventRestController(
        val policyCreationEventPublisher: InsuranceCreationEventPublisher
) {
    @GetMapping(
            path = ["/insurance/creation/error/{proposalId}"],
            produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun getErrors(@PathVariable("proposalId") proposalId: String): Publisher<String> {
        return policyCreationEventPublisher.ofErrorsForProposal(proposalId)
    }

    @GetMapping(
        path = ["/insurance/created/{proposalId}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun getInsuranceCreated(@PathVariable("proposalId") proposalId: String): Publisher<String> {
        return policyCreationEventPublisher.ofInsuranceCreatedForProposal(proposalId)
    }
}