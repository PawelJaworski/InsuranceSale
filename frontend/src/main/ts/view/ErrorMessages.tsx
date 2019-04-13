import * as React from "react";
import * as GET from "../model/proposal/GET";
import {ReconnectableEventSource} from "../util/ReconnectableEventSource";
import * as POST from "../model/proposal/POST";

const ErrorMessages = (props) => {
    const {messages} = props

    return <React.Fragment>
        {messages && messages.length > 0
            ? <div className="alert alert-danger">{messages.join("\n")}</div>
            : null
        }
    </React.Fragment>
}

export default ErrorMessages;

export class ServiceMessages extends React.Component <MessagesProps> {
    state = {
        messages: [],
    }
    componentDidMount(): void {
        const eventSource = new ReconnectableEventSource()
        eventSource.onConnectionAlive = () => {
            this.clearMessages()
        }
        eventSource.onMessage = (event: MessageEvent) => {
            const message = event.data
            this.display(message)
        }
        eventSource.onError = (error: MessageEvent) =>
            this.display(this.props.serviceName + " unavailable.")
        eventSource.connect(this.props.url)
    }

    private display(error: string) {
        this.state.messages = [error]
        this.setState({messages: this.state.messages})
    }

    private clearMessages() {
        this.state.messages= []
        this.setState({messages: this.state.messages})
    }

    render() {
        const {messages} = this.state
        return (
            <React.Fragment>
                {messages && messages.length > 0
                    ? <div className={this.props.className}>{messages.join("\n")}</div>
                    : null
                }
            </React.Fragment>
        );
    }
}

class MessagesProps {
    url: string
    serviceName: string
    className: string
}
