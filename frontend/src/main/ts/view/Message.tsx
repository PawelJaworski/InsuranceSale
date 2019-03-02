import * as React from "react";

const Message = (props) => {
    const {message} = props
    return <React.Fragment>
        {message
            ? <div className="alert alert-danger">{message}</div>
            : null
        }
    </React.Fragment>
}

export default Message;
