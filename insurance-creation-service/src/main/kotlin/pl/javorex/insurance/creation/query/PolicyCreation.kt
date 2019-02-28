package pl.javorex.insurance.creation.query

import reactor.core.publisher.Flux

interface PolicyCreation {
    fun fluxForProposalId(proposalId: String) : Flux<String>
}