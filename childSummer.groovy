// This Script will sum all epic children underneath and show it in a scripted field.

def issue;
def jqlSearchQuery = "parent = ${issue.key}";
def API_ = "/rest/api/3/search";
def childrenInEpic = 0;
post(API_)
.header('Content-Type', 'application/json').
body([
    jql: jqlSearchQuery
]).asObject(Map).body.issues.each {
    childrenInEpic++;
}

return childrenInEpic;