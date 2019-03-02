import axios from 'axios'

export function proposalAccepted(
    proposalId: string,
    version: number,
    insuranceProduct: string,
    numberOfPremiums: number
) {
    axios.post("/proposal/accept", {
        proposalId: proposalId,
        version: version,
        insuranceProduct: insuranceProduct,
        numberOfPremiums: numberOfPremiums
    }).then(res => {
        console.log(res);
        console.log(res.data);
    })
}