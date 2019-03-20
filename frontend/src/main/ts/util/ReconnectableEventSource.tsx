export class ReconnectableEventSource {
    private eventSource: EventSource

    onConnectionAlive: () => void = () => {}
    onOpen: (event: MessageEvent) => void = (event: MessageEvent) => {}
    onMessage: (event: MessageEvent) => void = (event: MessageEvent) => {}
    onError: (error: MessageEvent) => void = (error: MessageEvent) =>  {}

    constructor() {
        this.heartBeat()
    }

    heartBeat() {
        setTimeout(() => {
            if (this.eventSource.readyState == 0) {
                this.onConnectionAlive()
            }
            this.heartBeat()},
            2000
        )
    }
    connect(url: string) {
        this.eventSource = new EventSource(url)

        this.eventSource.onopen = this.onOpen
        this.eventSource.onmessage = this.onMessage
        this.eventSource.onerror = (error:MessageEvent) => {
            this.onError(error)
            setTimeout(() => {
                if (this.eventSource != undefined && this.eventSource.readyState == 2) {
                    //this.eventSource.close()
                    this.connect(url)
                }

            },
            2000)
        }
    }
}