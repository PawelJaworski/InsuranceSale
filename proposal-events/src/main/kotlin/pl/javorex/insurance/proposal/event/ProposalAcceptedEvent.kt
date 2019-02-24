package pl.javorex.insurance.proposal.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ProposalAcceptedEvent
@JsonCreator
constructor(
        @JsonProperty("proposalId") val proposalId: String,
        @JsonProperty("insuranceProduct") val insuranceProduct: String,
        @JsonProperty("numberOfPremiums") val numberOfPremiums: Int
)