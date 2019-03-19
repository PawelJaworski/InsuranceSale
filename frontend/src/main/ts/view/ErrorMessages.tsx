import * as React from "react";

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
