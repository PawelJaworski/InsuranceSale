package pl.javorex.insurance.creation.adapter.kafka

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.Stores
import pl.javorex.event.util.EventSagaTemplate
import pl.javorex.kafka.streams.event.EventEnvelopeSerde
import pl.javorex.util.kafka.common.serialization.JsonPojoSerde

internal const val PROPOSAL_EVENTS_SOURCE = "Proposal-Events-Source"
internal const val INSURANCE_EVENTS_SOURCE = "Insurance-Events-Source"
internal const val PREMIUM_EVENTS_SOURCE = "Premium-Events-Source"

internal const val PROPOSAL_ACCEPTED_UNIQUE_EVENT_PROCESSOR = "Proposal-Accepted-Unique-Event-Processor"
internal const val INSURANCE_CREATION_SAGA_PROCESSOR = "Insurance-Creation-Saga-Processor"

internal const val UNIQUE_PROPOSAL_ACCEPTED_STORE = "Unique-Proposal-Accepted-Store"
internal const val INSURANCE_CREATION_STORE = "Insurance-Creation-Saga-Store"

internal const val INSURANCE_CREATION_SINK = "Insurance-Creation-Sink"
internal const val INSURANCE_CREATION_ERROR_SINK = "Insurance-Creation-Error-Sink"

private val insuranceStoreSupplier: KeyValueBytesStoreSupplier = Stores
        .persistentKeyValueStore(INSURANCE_CREATION_STORE)
internal val insuranceStoreBuilder = Stores
        .keyValueStoreBuilder(insuranceStoreSupplier, Serdes.String(), JsonPojoSerde(EventSagaTemplate::class.java))
private val proposalAcceptedUniqueEventStoreSupplier: KeyValueBytesStoreSupplier = Stores
        .persistentKeyValueStore(UNIQUE_PROPOSAL_ACCEPTED_STORE)
internal val proposalAcceptedUniqueEventStoreBuilder = Stores
        .keyValueStoreBuilder(proposalAcceptedUniqueEventStoreSupplier, Serdes.String(), EventEnvelopeSerde())
