
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchProviderFactory
import com.atlassian.jira.issue.util.IssueIdsIssueIterable
import com.atlassian.jira.jql.builder.JqlQueryBuilder
import com.atlassian.jira.jql.query.IssueIdCollector
import com.atlassian.jira.task.context.LoggingContextSink
import com.atlassian.jira.task.context.PercentageContext
import com.atlassian.jira.issue.Issue;
import com.onresolve.jira.groovy.user.FieldBehaviours;
import com.onresolve.jira.groovy.user.FormField;
import groovy.transform.BaseScript;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.IssueImpl;
import java.time.LocalDateTime;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.IssueLink;
@BaseScript FieldBehaviours fieldBehaviours;


Issue issue = event.issue; // the updated issue .

// ---- CONST Variables => 

// FEATURE => EPIC ISSUE PICKERS
def RELATED_FEATURE_TO_EPIC = "customfield_14004";
def RELATED_EPICS_TO_FEATURES = "customfield_14005";
// ALLOCATION => FEATURE ISSUE PICKERS
def RELATED_ALLOCATIONS_TO_FEATURE_FIELD = "customfield_14024";
def RELATED_FEATURE_TO_ALLOCATION_FIELD = "customfield_14023";

// --- global variables =>
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager);
def availableIssueLinkTypes = issueLinkTypeManager.issueLinkTypes;
def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
def linkManager = ComponentAccessor.getIssueLinkManager();
def issueManager = ComponentAccessor.getIssueManager();
def issueLink;
def inputIssues;
def issuePickerAllocation;
def allocationInputIssues;
def currentValueForInnerIssuePicker;
def outward;
def issueLinkName;
def linkType;

MutableIssue innerIssue;
MutableIssue tempIssue;
MutableIssue allocationIssue;
// Generic Decleration a global Variable, generic Setting a value according to the type of the updated issue;

switch (getIssueType(issue)) {
    case "Epic":
        // Feature Issue Picker To Feature;
        issueLink = customFieldManager.getCustomFieldObject(RELATED_FEATURE_TO_EPIC);
        // Feature value as a String
        inputIssues = issue.getCustomFieldValue(issueLink) as Collection;
        break;

    case "Feature":
        // Epic Issue Picker To Feature;
        issueLink = customFieldManager.getCustomFieldObject(RELATED_EPICS_TO_FEATURES);
        // Epic value as an Array
        inputIssues = issue.getCustomFieldValue(issueLink) as Collection;

        issuePickerAllocation = customFieldManager.getCustomFieldObject(RELATED_ALLOCATIONS_TO_FEATURE_FIELD);
        allocationInputIssues = issue.getCustomFieldValue(issuePickerAllocation) as Collection;
        break;

    case "Allocation":
        issuePickerAllocation = customFieldManager.getCustomFieldObject(RELATED_FEATURE_TO_ALLOCATION_FIELD); // Feature Issue Picker To Feature;
        allocationInputIssues = issue.getCustomFieldValue(issuePickerAllocation) as Collection; // Feature value as a String    
        break;

    default: return;
}


if (getIssueType(issue) == "Feature") {
    outward = linkManager.getInwardLinks(issue.getId()) as Collection<IssueLink>;

} else {
    outward = linkManager.getOutwardLinks(issue.getId()) as Collection<IssueLink>;
}

// if issue picker value is empty - Setting an empty Array;  =====>
// We dont want to run throught this array below if there is no value here;

inputIssues = (!inputIssues || inputIssues.size() == 0) ? [] : inputIssues;
allocationInputIssues = (!allocationInputIssues || allocationInputIssues.size() == 0) ? [] : allocationInputIssues;

switch (getIssueType(issue)) {
    case "Feature":

        for (def currentEpicChild = 0; currentEpicChild < inputIssues.size(); currentEpicChild++) {

            innerIssue = issueManager.getIssueObject(inputIssues[currentEpicChild].toString());

            issueLinkName = setIssueLinkNameByUpdatedIssueType(innerIssue, issue) as String; // Getting the issue type link name by a funtion written Below. will use the link type ID below.
            linkType = availableIssueLinkTypes.findByName(issueLinkName);

            innerIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_FEATURE_TO_EPIC), issue);
            createIssueLinkAndUpdateIssue(issue, innerIssue, linkType.id);
        }

        for (def currentAllocation = 0; currentAllocation < allocationInputIssues.size(); currentAllocation++) {

            allocationIssue = issueManager.getIssueObject(allocationInputIssues[currentAllocation].toString());
            allocationIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_FEATURE_TO_ALLOCATION_FIELD), issue);
            issueLinkName = setIssueLinkNameByUpdatedIssueType(allocationIssue, issue) as String; // Getting the issue type link name by a function written Below. will use the link type ID below.
            linkType = availableIssueLinkTypes.findByName(issueLinkName);
            createIssueLinkAndUpdateIssue(issue, allocationIssue, linkType.id);

        }

        break;


    case "Epic":

        innerIssue = issueManager.getIssueObject(inputIssues[0].toString());

        issueLinkName = setIssueLinkNameByUpdatedIssueType(innerIssue, issue) as String; // Getting the issue type link name by a funtion written Below. will use the link type ID below.
        linkType = availableIssueLinkTypes.findByName(issueLinkName);

        currentValueForInnerIssuePicker = innerIssue.getCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_EPICS_TO_FEATURES)) as Collection;

        if (!currentValueForInnerIssuePicker || currentValueForInnerIssuePicker.size() == 0) {

            currentValueForInnerIssuePicker = [issue];

        } else {

            currentValueForInnerIssuePicker.add(issue);
        }
        //removeIssueLink(issue, inputIssues);

        innerIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_EPICS_TO_FEATURES), currentValueForInnerIssuePicker);
        createIssueLinkAndUpdateIssue(issue, innerIssue, linkType.id);
        break;

    case "Allocation":

        innerIssue = issueManager.getIssueObject(allocationInputIssues[0].toString());
        issueLinkName = setIssueLinkNameByUpdatedIssueType(innerIssue, issue) as String; // Getting the issue type link name by a funtion written Below. will use the link type ID below.
        linkType = availableIssueLinkTypes.findByName(issueLinkName);
        currentValueForInnerIssuePicker = innerIssue.getCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_ALLOCATIONS_TO_FEATURE_FIELD)) as Collection;
        if (!currentValueForInnerIssuePicker || currentValueForInnerIssuePicker.size() == 0) {

            currentValueForInnerIssuePicker = [issue];

        } else {

            currentValueForInnerIssuePicker.add(issue);

        }

        innerIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_ALLOCATIONS_TO_FEATURE_FIELD), currentValueForInnerIssuePicker);
        createIssueLinkAndUpdateIssue(issue, innerIssue, linkType.id);

        break;
}



for (def i = 0; i < outward.size(); i++) {
    tempIssue = issueManager.getIssueObject(outward[i].sourceId);
    for (def j = 0; j < inputIssues.size(); j++) {

        if (tempIssue.toString() != inputIssues[j].toString()) {
            linkManager.removeIssueLink(outward[i], user);
            tempIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_FEATURE_TO_EPIC), null)
            issueManager.updateIssue(user, tempIssue, EventDispatchOption.DO_NOT_DISPATCH, false);

        }
    }

}

//for (def i = 0; i < outward.size(); i++) {
//  tempIssue = issueManager.getIssueObject(outward[i].sourceId);
//for (def j = 0; j < allocationInputIssues.size(); j++) {

//  if (tempIssue.toString() != allocationInputIssues[j].toString()) {
//    linkManager.removeIssueLink(outward[i], user);
//  tempIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_FEATURE_TO_EPIC), null)
//issueManager.updateIssue(user, tempIssue, EventDispatchOption.DO_NOT_DISPATCH, false);

//}
//}


//}

updateCurrentIssue(issue);



private void updateCurrentIssue(Issue issue) {
    def issueManager = ComponentAccessor.getIssueManager();
    def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
    MutableIssue currentIssue = issueManager.getIssueObject(issue.id);
    issueManager.updateIssue(user, currentIssue, EventDispatchOption.ISSUE_UPDATED, false);


}

private void createIssueLinkAndUpdateIssue(Issue issue, MutableIssue innerIssue, linkTypeId) {
    def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
    def linkManager = ComponentAccessor.getIssueLinkManager();
    def issueManager = ComponentAccessor.getIssueManager();


    if (getIssueType(issue) != "Feature") {
        linkManager.createIssueLink(issue.getId() as Long, innerIssue.getId() as Long, linkTypeId as Long, 1 as Long, user); // Setting Jira's Main Linked Issues by givven value;

    } else {
        linkManager.createIssueLink(innerIssue.getId() as Long, issue.getId() as Long, linkTypeId as Long, 1 as Long, user); // Setting Jira's Main Linked Issues by givven value;

    }

    issueManager.updateIssue(user, innerIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
    log.warn(innerIssue)
}


private void removeIssueLink(Issue issue, inputIssues){

    def linkManager = ComponentAccessor.getIssueLinkManager();
    def issueManager = ComponentAccessor.getIssueManager();
    def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
    def outward = linkManager.getOutwardLinks(issue.getId()) as Collection<IssueLink>;

    for (def i = 0; i < outward.size(); i++) {

        MutableIssue tempIssue = issueManager.getIssueObject(outward[i].sourceId);
        linkManager.removeIssueLink(outward[i], user);
    }
}

private String setIssueLinkNameByUpdatedIssueType(MutableIssue innerIssue, Issue issue) {
    def issueLinkName;

    if (getIssueType(issue) == "Feature" && getIssueType(innerIssue) == "Epic" || getIssueType(issue) == "Epic" && getIssueType(innerIssue) == "Feature") {

        issueLinkName = "Feature-Epics";

    } else if (getIssueType(issue) == "Feature" && getIssueType(innerIssue) == "Allocation" || getIssueType(issue) == "Allocation" && getIssueType(innerIssue) == "Feature") {

        issueLinkName = "Allocation - Feature";
    }
    log.warn(issueLinkName);
    return issueLinkName;

}


private String getIssueType(Issue issue){

    return issue.getIssueType()?.getName();
}
