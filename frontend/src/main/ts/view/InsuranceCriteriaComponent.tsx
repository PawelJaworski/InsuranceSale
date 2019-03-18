import * as React from "react";
import Messages from "./Messages";
import * as POST from "../model/proposal/POST";
import * as GET from "../model/proposal/GET";
import {ReconnectableEventSource} from "../util/ReconnectableEventSource";

const PROPOSAL_ID = "proposal-1"
export default class InsuranceCriteriaComponent extends React.Component <{}> {
    state = {
        error: []
    }

    versionId: number = undefined
    constructor(props) {
        super(props)
        GET.nextProposalVersion()
            .then(versionId => this.versionId  = versionId)
    }

    componentDidMount(): void {
        const eventSource = new ReconnectableEventSource()
        eventSource.beforeConnect = () => {
            this.clearErrors()
        }
        eventSource.onMessage = (event: MessageEvent) => {
            const message = event.data
            this.displayError(message)
        }
        eventSource.onError = (error: any) =>  this.displayError("Policy service error")
        eventSource.connect("/insurance/creation/error/" + PROPOSAL_ID)
    }

    private displayError(error: string) {
        this.state.error.push(error)
        this.setState({error: this.state.error})
    }

    private clearErrors() {
        this.state.error = []
        this.setState({error: this.state.error})
    }


    private onSubmit = (event) => {
        POST.proposalAccepted(PROPOSAL_ID, this.versionId, "GREAT_PRODUCT", 12)
    }
    render() {
        return (
            <div className="container">
                <div className="row centered">
                    <div className={rowCss}>
                        <Messages messages = {this.state.error}/>
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