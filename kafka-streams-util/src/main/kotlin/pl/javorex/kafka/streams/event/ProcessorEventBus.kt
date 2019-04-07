package pl.javorex.kafka.streams.event

import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.To
import pl.javorex.event.util.SagaEventBus
import pl.javorex.event.util.pack

class ProcessorEventBus(
        private val context: ProcessorContext,
        private val sinkName: String,
        private val errorSinkName: String

) : SagaEventBus {
    override fun emitError(aggregateId: String, aggregateVersion: Long, event: Any) {
        val eventEnvelope = pack(aggregateId, aggregateVersion, event)
        context.forward(aggregateId, eventEnvelope, To.child(errorSinkName))
    }

    override fun emit(aggregateId: String, aggregateVersion: Long, event: Any) {
        val eventEnvelope = pack(aggregateId, aggregateVersion, event)
        context.forward(aggregateId, eventEnvelope, To.child(sinkName))}

}