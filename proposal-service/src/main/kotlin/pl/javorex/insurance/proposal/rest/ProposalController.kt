package pl.javorex.insurance.proposal.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.javorex.insurance.proposal.application.ProposalEventBus
import pl.javorex.insurance.proposal.event.ProposalAccepted
import pl.javorex.insurance.proposal.rest.command.AcceptProposalCommand

@RestController
class ProposalController(
        private val proposalEventBus: ProposalEventBus
) {
    @PostMapping(path = ["/proposal/accept"])
    fun acceptProposal(@RequestBody command: AcceptProposalCommand): ResponseEntity<Any> {
        val event = ProposalAccepted(
                command.proposalId,
                command.insuranceProduct,
                command.numberOfPremiums
        )
        proposalEventBus.emit(event, command.version)

        return ResponseEntity.ok(HttpStatus.OK)
    }
}

