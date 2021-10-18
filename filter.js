const axios = require("axios");
const config = require("../config.json");

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
