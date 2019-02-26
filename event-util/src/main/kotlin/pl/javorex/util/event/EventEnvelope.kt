package pl.javorex.util.event

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule


data class EventEnvelope
@JsonCreator
constructor(
        @JsonProperty("aggregateId") val aggregateId: String,
        @JsonProperty("aggregateVersion") val aggregateVersion: Long,
        @JsonProperty("eventType") val eventType: String,
        @JsonProperty("payload") val payload: JsonNode) {

    fun isTypeOf(clazz: Class<*>) = clazz.simpleName == eventType

    fun <T>unpack(clazz: Class<T>): T = newObjectMapper().treeToValue(payload, clazz)

    fun repack(event: Any): EventEnvelope {
        return pack(aggregateId, aggregateVersion, event)
    }
}

fun pack(aggregateId: String, aggregateVersion: Long, event: Any): EventEnvelope {
    val eventType = event::class.java.simpleName
    val payload = newObjectMapper().convertValue(event, JsonNode::class.java)

    return EventEnvelope(aggregateId, aggregateVersion, eventType, payload)
}

private fun newObjectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(ParameterNamesModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)