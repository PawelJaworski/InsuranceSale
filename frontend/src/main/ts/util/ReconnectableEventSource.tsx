export class ReconnectableEventSource {
    private eventSource: EventSource

    beforeConnect: () => void
    onOpen: (event: MessageEvent) => void = (event: MessageEvent) => {}
    onMessage: (event: MessageEvent) => void = (event: MessageEvent) => {}
    onError: (error: MessageEvent) => void = (error: MessageEvent) =>  {}

    connect(url: string) {
        this.beforeConnect()
        this.eventSource = new EventSource(url)
        this.eventSource.onopen = this.onOpen
        this.eventSource.onmessage = this.onMessage
        this.eventSource.onerror = (error:MessageEvent) => {
            this.onError(error)
            setTimeout(() => {
                if (this.eventSource != undefined && this.eventSource.readyState == this.eventSource.CLOSED) {
                    this.eventSource.close()
                }
                this.connect(url)
            },
            2000)
        }
    }
}