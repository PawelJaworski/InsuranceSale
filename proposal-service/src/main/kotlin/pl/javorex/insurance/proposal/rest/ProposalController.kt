package pl.javorex.insurance.proposal.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.javorex.insurance.proposal.application.EventBus
import pl.javorex.insurance.proposal.event.ProposalAcceptedEvent
import pl.javorex.insurance.proposal.rest.command.AcceptProposalCommand

@RestController
class ProposalController(
        private val eventBus: EventBus
) {
    @PostMapping(path = ["/proposal/accept"])
    @ResponseStatus(value= HttpStatus.OK)
    fun acceptProposal(@RequestBody command: AcceptProposalCommand): ResponseEntity<Any> {
        val event = ProposalAcceptedEvent(
                command.proposalId,
                command.insuranceProduct,
                command.numberOfPremiums
        )
        eventBus.emit(event, command.version)

        return ResponseEntity.ok(HttpStatus.OK)
    }
}

