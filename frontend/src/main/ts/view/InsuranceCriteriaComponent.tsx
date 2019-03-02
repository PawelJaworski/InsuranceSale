import * as React from "react";
import Message from "./Message";

export default class InsuranceCriteriaComponent extends React.Component <{}> {
    state = {
        error: "",
    }

    componentDidMount(): void {
        const eventSource = new EventSource("/insurance/created/1");
        eventSource.onopen = (event: MessageEvent) => {
            this.state.error = ""
            this.setState({error: this.state.error})
        }

        eventSource.onmessage = (event: MessageEvent) => {
            this.state.error = event.data;
            this.setState({error: this.state.error});
        }
        eventSource.onerror = (error: any) =>  this.displayError("Policy service error")
    }

    private displayError(error: string) {
        this.state.error = error
        this.setState({error: this.state.error})
    }
    render() {
        return (
            <div className="container">
                <div className="row centered">
                    <div className={rowCss}>
                        <Message message = {this.state.error}/>
                    </div>
                    <div className={rowCss}>
                        <h1>Insurance Criteria</h1>
                    </div>
                </div>
            </div>
        );
    }
}

const rowCss = "col-md-6 offset-md-3 text-center"