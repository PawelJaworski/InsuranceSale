import * as React from "react";

export default class InsuranceForm extends React.Component <{}> {
    state = {
        error: '',
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
        };
        eventSource.onerror = (error: any) => {
            this.state.error = "Policy service error"
            this.setState({error: this.state.error})
        }
    }

    private tryInit() {

    }
    render() {
        return (
            <div className="container">
                <div className="row centered">
                    <div className="col-md-6 offset-md-3 text-center">
                        {this.state.error
                            ? <div className="alert alert-danger">{this.state.error}</div>
                            : null
                        }

                        <h1>Insurance Criteria</h1>
                    </div>
                </div>
            </div>
        );
    }
}