const axios = require("axios");
const JiraApi = require("jira").JiraApi;
const config = require("../config.json");


let jira = new JiraApi(config.type, config.host, config.port, config.username, config.password, config.version);
async function createFilter(filterFields) {
    const header = {
        headers: {
            "user-agent": config.userAgent,
            "content-type": "application/json"
        }
    }
    let filter = {};
    try {
        filter = await axios.post("http://localhost:8080/rest/api/2/filter/", {body : filterFields}, header)
        .then((res) => {
            console.log(filter);
        }).catch((err) => {
            console.log(err);
        });
    } catch (error) {
    console.log(filter);
        
        throw new Error(error);
    }
    return filter
}

module.exports = {
    createFilter
}
