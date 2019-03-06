package pl.javorex.insurance.creation.rest

import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import pl.javorex.insurance.creation.query.PolicyCreation
import reactor.core.publisher.Flux


@RestController
class PolicyCreationFluxController(
        val policyCreation: PolicyCreation
) {
    @GetMapping(
            path = ["/insurance/creation/error/{proposalId}"],
            produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun getErrors(@PathVariable("proposalId") proposalId: String): Flux<String> {
        return policyCreation.fluxForProposalId(proposalId)
    }
}