
//  Global Variables to be used in the code below.
def API_ = "/rest/api/3/issue";
def progressedDateFieldId = "customfield_10057";
def now = new Date().format( 'yyyy-MM-dd' )

logger.info(issue.fields[progressedDateFieldId])


//  If the current field already has a value in it, dont execute the code below.
if(issue.fields[progressedDateFieldId]) return;

// The actual http request.
put("${API_}/${issue.key}")
    .header('Content-Type', 'application/json')
    .body([
        fields: [
            (progressedDateFieldId): now
        ]
    ]).asString()