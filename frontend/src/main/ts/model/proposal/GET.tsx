import axios from "axios";
import {any, number} from "prop-types";

export function nextProposalVersion() : Promise<number> {
    return Promise.resolve(new Date().getTime())
}