package pl.javorex.event.util

interface UnambiguousEventVersion {
    val aggregateId: String
    val aggregateVersion: Long
}