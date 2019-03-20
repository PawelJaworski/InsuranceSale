package pl.javorex.event.saga

interface EventSagaError

class OtherRequestAlreadyPending : EventSagaError
data class DoubleEvent(val eventType: String)
class RequestTimeout