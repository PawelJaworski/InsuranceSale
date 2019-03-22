import * as React from "react";
import ErrorMessages from "./ErrorMessages";
import * as POST from "../model/proposal/POST";
import * as GET from "../model/proposal/GET";
import {ReconnectableEventSource} from "../util/ReconnectableEventSource";

const PROPOSAL_ID = "proposal-1"
export default class InsuranceCriteriaComponent extends React.Component <{}> {
    state = {
        proposalError: [],
        insuranceCreationError: []
    }

    versionId: number = undefined
    constructor(props) {
        super(props)
        GET.nextProposalVersion()
            .then(versionId => this.versionId  = versionId)
    }

    componentDidMount(): void {
        const eventSource = new ReconnectableEventSource()
        eventSource.onConnectionAlive = () => {
            this.clearInsuranceCreationErrors()
        }
        eventSource.onMessage = (event: MessageEvent) => {
            const message = event.data
            this.displayInsuranceCreationError(message)
        }
        eventSource.onError = (error: MessageEvent) =>
            this.displayInsuranceCreationError("Insurance-Creation-Service unavailable.")
        eventSource.connect("/insurance/creation/error/" + PROPOSAL_ID)
    }

    private displayProposalError(error: string) {
        this.state.proposalError = [error]
        this.setState({proposalError: this.state.proposalError})
    }

    private displayInsuranceCreationError(error: string) {
        this.state.insuranceCreationError = [error]
        this.setState({insuranceCreationError: this.state.insuranceCreationError})
    }

    private clearProposalError() {
        this.state.proposalError = []
        this.setState({proposalError: this.state.proposalError  })
    }


    private clearInsuranceCreationErrors() {
        this.state.insuranceCreationError = []
        this.setState({insuranceCreationError: this.state.insuranceCreationError})
    }


    private onSubmit = (event) => {
        POST.proposalAccepted(PROPOSAL_ID, this.versionId, "GREAT_PRODUCT", 1)
            .catch(reason => {
                this.clearProposalError()
                this.displayProposalError("Proposal-Service " + reason + ".")
            })
    }
    render() {
        return (
            <div className="container">
                <div className="row centered">
                    <div className={rowCss}>
                        <ErrorMessages messages = {this.state.proposalError}/>
                    </div>
                    <div className={rowCss}>
                        <ErrorMessages messages = {this.state.insuranceCreationError}/>
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