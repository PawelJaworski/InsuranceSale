import * as React from "react";
import axios from "axios"
import Message from "./Message";
import {proposalAccepted} from "../model/proposal/POST";

const PROPOSAL_ID = "proposal-1"
export default class InsuranceCriteriaComponent extends React.Component <{}> {
    state = {
        error: "",
    }

    versionId: number = undefined
    constructor(props) {
        super(props)
        this.versionId = new Date().getTime()
    }

    componentDidMount(): void {
        const eventSource = new EventSource("/insurance/creation/error/" + PROPOSAL_ID);
        eventSource.onopen = (event: MessageEvent) => {
            this.state.error = ""
            this.setState({error: this.state.error})
        }

        eventSource.onmessage = (event: MessageEvent) => {
            const message = event.data
            console.log("messaging " + message)
            this.state.error = message;
            this.setState({error: this.state.error});
        }
        eventSource.onerror = (error: any) =>  this.displayError("Policy service error")
    }

    private displayError(error: string) {
        console.log("displaying " + error)
        this.state.error = error
        this.setState({error: this.state.error})
    }

    private onSubmit = (event) => {
        proposalAccepted(PROPOSAL_ID, this.versionId, "GREAT_PRODUCT", 12)
    }
    render() {
        return (
            <div className="container">
                <div className="row centered">
                    <div className={rowCss}>
                        <Message message = {this.state.error}/>
                    </div>
                    <div className={rowCss}>
                        <h1 className="card-header">
                            Insurance Criteria
                        </h1>
                    </div>
                    <div className={rowCss}>
                        <button className="btn btn-primary" onClick={this.onSubmit}>Submit</button>
                    </div>
                </div>
            </div>
        );
    }
}

const rowCss = "col-md-6 offset-md-3 text-center"