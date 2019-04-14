package pl.javorex.event.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule

data class EventEnvelope(
        override val aggregateId: String,
        override val aggregateVersion: Long,
        val timestamp: Long,
        val eventType: String,
        val payload: JsonNode
) : UnambiguousEventVersion {
    fun isTypeOf(clazz: Class<*>) = clazz.simpleName == eventType
    fun <T>unpack(clazz: Class<T>): T = newObjectMapper().treeToValue(payload, clazz)
    fun withTimestamp(timestamp: Long) = EventEnvelope(aggregateId, aggregateVersion, timestamp, eventType, payload)
}

fun repack(other: EventEnvelope, event: Any) =
        pack(other.aggregateId, other.aggregateVersion, event)

fun pack(aggregateId: String, aggregateVersion: Long, event: Any): EventEnvelope {
    val eventType = event::class.java.simpleName
    val payload = newObjectMapper().convertValue(event, JsonNode::class.java)

    return EventEnvelope(aggregateId, aggregateVersion, System.currentTimeMillis(), eventType, payload)
}

private fun newObjectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(ParameterNamesModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)