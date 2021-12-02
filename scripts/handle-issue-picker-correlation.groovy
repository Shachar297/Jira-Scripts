import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.onresolve.jira.groovy.user.FieldBehaviours;


Issue currentIssue = event.issue

// This script only runs if epic issue picker to pick environments or environment issue picker to pick epic issue are changed.
def history = event.getChangeLog().getRelated("ChildChangeItem")
if (!"Epics" in history.field || !"Environment" in history.field) {
    return
}

if(currentIssue.getIssueType().name != "Epic" && currentIssue.getIssueType().name != "Environment") {
    return
}

// =============================================

def issueManager = ComponentAccessor.getIssueManager();
def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
def customFieldManager = ComponentAccessor.getCustomFieldManager();

// =============================================
def changeHistoryPickerValue;
def environmentIssuePicker = customFieldManager.getCustomFieldObject("customfield_14320")
def epicInEnviromentIssuePicker = customFieldManager.getCustomFieldObject("customfield_14324");

if(currentIssue.getIssueType().name == "Epic") {

    changeHistoryPickerValue = history.find {
        it.field == "Environment"
    }
    
    onEpicDeleteIssueFromIssuePicker(currentIssue, changeHistoryPickerValue.oldstring, currentIssue.getCustomFieldValue(environmentIssuePicker) as Collection, environmentIssuePicker, issueManager, user, epicInEnviromentIssuePicker)
    
    onEpicChanges(currentIssue, environmentIssuePicker, epicInEnviromentIssuePicker, issueManager, user);

}else if (currentIssue.getIssueType().name == "Environment") {


    changeHistoryPickerValue = history.find {
        it.field == "Epics"
    }
	
    // When Deleting a value from the picker, this function delete the value from both side (env -> epic)
    onEnvironmentDeletedIssueFromPicker(currentIssue, changeHistoryPickerValue.oldstring, currentIssue.getCustomFieldValue(epicInEnviromentIssuePicker) as Collection, environmentIssuePicker, issueManager, user)
	
    // When adding a value to the picker, add the value both sided (env -> epic)
    onEnvIssueChanges(currentIssue, environmentIssuePicker, epicInEnviromentIssuePicker, issueManager, user);

}

private void onEpicDeleteIssueFromIssuePicker(Issue currentIssue, oldValue,Collection newValues1, CustomField environmentIssuePicker, IssueManager issueManager, ApplicationUser user, CustomField epicInEnviromentIssuePicker) {
    
    def oldValues = oldValue.toString().split(",");
    def newValues = newValues1.toString().split(",");
    
    if((oldValues.size() > newValues.size() || oldValues.size() != newValues.size()) || newValues.toString().indexOf("null") != -1) {

        for(def i = 0; i < oldValues.size(); i ++ ) {
            oldValues[i] = trimElements(oldValues[i]);
        }

        for(def i = 0; i < newValues.size(); i ++ ) {
            newValues[i] = trimElements(newValues[i]);
        }
        
        def envToRemoveEpicIssueFrom = (oldValues - newValues) as Set
        def epicEnvironmentsList = [] as Collection<Issue>;

        for(def currentEnvIssue = 0; currentEnvIssue < envToRemoveEpicIssueFrom.size(); currentEnvIssue ++ ) {
            MutableIssue environmentIssue = issueManager.getIssueObject(envToRemoveEpicIssueFrom[currentEnvIssue].toString());
            def epicIssueCurrentEnvIssuePickerValue = environmentIssue.getCustomFieldValue(epicInEnviromentIssuePicker) as Collection;
            epicEnvironmentsList = (epicIssueCurrentEnvIssuePickerValue - [currentIssue]);
            
            environmentIssue.setCustomFieldValue(epicInEnviromentIssuePicker, epicEnvironmentsList);
            issueManager.updateIssue(user, environmentIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
        }
    }
}


private boolean onEnvironmentDeletedIssueFromPicker(Issue currentIssue, oldValue,Collection newValues1, CustomField environmentIssuePicker, IssueManager issueManager, ApplicationUser user) {

    def oldValues = oldValue.toString().split(",");
    def newValues = newValues1.toString().split(",");

    if((oldValues.size() > newValues.size()|| oldValues.size() != newValues.size()) || newValues.toString().indexOf("null") != -1) {

        for(def i = 0; i < oldValues.size(); i ++ ) {
            oldValues[i] = trimElements(oldValues[i]);
        }

        for(def i = 0; i < newValues.size(); i ++ ) {
            newValues[i] = trimElements(newValues[i]);
        }

        def epicToRemoveEnvIssueFrom = (oldValues - newValues) as Set
        def epicEnvironmentsList = [] as Collection<Issue>;

        for(def currentEpicIssue = 0; currentEpicIssue < epicToRemoveEnvIssueFrom.size(); currentEpicIssue ++ ) {
            MutableIssue epicIssue = issueManager.getIssueObject(epicToRemoveEnvIssueFrom[currentEpicIssue].toString());
            def epicIssueCurrentEnvIssuePickerValue = epicIssue.getCustomFieldValue(environmentIssuePicker) as Collection;
            epicEnvironmentsList = (epicIssueCurrentEnvIssuePickerValue - [currentIssue]);
            
            epicIssue.setCustomFieldValue(environmentIssuePicker, epicEnvironmentsList);
            issueManager.updateIssue(user, epicIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
        }
    }
}

// Setting an array to a string and concat un wanted strings from it to perform a search.
def trimElements(String element) {
    if (element.contains("[")) {
        element = element.minus("[")
    }
    if (element.contains("]")) {
        element = element.minus("]")
    }
	
    // trim element (Remove spaces from index).
    return element.replace(" ", "");
}


private void onEnvIssueChanges(Issue currentIssue, CustomField environmentIssuePicker, CustomField epicInEnviromentIssuePicker, IssueManager issueManager, ApplicationUser user) {

    def relatedEpics = [] as Collection<MutableIssue>;
    def relaredEnvsInEpicIssue = [] as Collection<Issue>;
    relatedEpics = currentIssue.getCustomFieldValue(epicInEnviromentIssuePicker) ? currentIssue.getCustomFieldValue(epicInEnviromentIssuePicker) as Collection<MutableIssue>: []
    MutableIssue relatedEpicIssue;

    for(def currentEpic = 0; currentEpic < relatedEpics.size(); currentEpic ++ ) {

        relatedEpicIssue = issueManager.getIssueObject(relatedEpics[currentEpic].toString());
        relaredEnvsInEpicIssue = relatedEpicIssue.getCustomFieldValue(environmentIssuePicker) ? relatedEpicIssue.getCustomFieldValue(environmentIssuePicker) as Collection<Issue>: []
        relaredEnvsInEpicIssue += currentIssue;
        relatedEpicIssue.setCustomFieldValue(environmentIssuePicker, relaredEnvsInEpicIssue);
        issueManager.updateIssue(user, relatedEpicIssue, EventDispatchOption.DO_NOT_DISPATCH, false);

    }
}

// Handle changes when epic issue is chaning - set picker value on both sides (environment and epic).
private void onEpicChanges(Issue currentIssue, CustomField environmentIssuePicker, CustomField epicInEnviromentIssuePicker, IssueManager issueManager, ApplicationUser user) {

    MutableIssue epicInEnvironmentIssue;
    MutableIssue environmentIssue;

    def epicsInEnvironmentIssueAsArray = [] as Collection<Issue>;   
    def relatedEnvironments = [] as Collection<Issue>;

    relatedEnvironments = currentIssue.getCustomFieldValue(environmentIssuePicker) ? currentIssue.getCustomFieldValue(environmentIssuePicker) as Collection<Issue>: []

    for (def currentEnvIssue = 0; currentEnvIssue < relatedEnvironments.size(); currentEnvIssue ++ ) {

        environmentIssue = issueManager.getIssueObject(relatedEnvironments[currentEnvIssue].toString());
        environmentIssue.setCustomFieldValue(epicInEnviromentIssuePicker, [currentIssue]);

        issueManager.updateIssue(user, environmentIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
        setIssuePickerValueForEnvironment(environmentIssue, epicInEnviromentIssuePicker,environmentIssuePicker, epicsInEnvironmentIssueAsArray, epicInEnvironmentIssue,issueManager)

    }
}


private void setIssuePickerValueForEnvironment(MutableIssue envIssue, CustomField epicInEnviromentIssuePicker,CustomField environmentIssuePicker, Collection epicsInEnvironmentIssueAsArray, MutableIssue epicInEnvironmentIssue, IssueManager issueManager) {
    def newEpicsInEnv = [] as Collection<Issue>;
    def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();

    if(!envIssue) return;

    epicsInEnvironmentIssueAsArray = envIssue.getCustomFieldValue(epicInEnviromentIssuePicker) ? envIssue.getCustomFieldValue(epicInEnviromentIssuePicker) as Collection <Issue>: [];

        for(def currentEpicUnderEnv = 0; currentEpicUnderEnv < epicsInEnvironmentIssueAsArray.size(); currentEpicUnderEnv ++ ) {
            epicInEnvironmentIssue = issueManager.getIssueObject(epicsInEnvironmentIssueAsArray[currentEpicUnderEnv].toString());

            if(epicInEnvironmentIssue && epicInEnvironmentIssue.getCustomFieldValue(environmentIssuePicker)) {

                newEpicsInEnv.add(epicInEnvironmentIssue)
            }
        }
    envIssue.setCustomFieldValue(epicInEnviromentIssuePicker, newEpicsInEnv);
    issueManager.updateIssue(user, envIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
}

