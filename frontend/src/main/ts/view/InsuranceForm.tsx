import * as React from "react";

export default class InsuranceForm extends React.Component <{}> {
    state = {
        message: '',
    }

    componentDidMount(): void {
        const eventSource = new EventSource("/insurance/created/1");
        eventSource.onopen = (event: MessageEvent) => console.log('open', event);
        eventSource.onmessage = (event: MessageEvent) => {
            this.state.message = event.data;
            this.setState({message: this.state.message});
        };
        eventSource.onerror = (event: any) => console.log('error', event);
    }

    render() {
        return (
            <div>
                <div>{this.state.message}</div>
                <h1>Insurance Criteria</h1>
            </div>
        );
    }
}