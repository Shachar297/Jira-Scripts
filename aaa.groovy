

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


def RELATED_INITIATIVE_ID = "customfield_14006";
def RELATED_FEATURES_ID = "customfield_14002";

def FEATURE_ISSUE_PICKER_ID = "customfield_14004";
def EPIC_ISSUE_PICKER_ID = "customfield_14005";

// --- global variables =>

def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
def linkManager = ComponentAccessor.getIssueLinkManager();
def issueManager = ComponentAccessor.getIssueManager();
def issueLink;
// Generic Decleration a global Variable, generic Setting a value according to the type of the updated issue;


if(getIssueType(issue) == "Feature"){
                //def relatedInitiativeToFeate = customFieldManager.getCustomFieldObject(RELATED_EPICS_TO_FEATURE); // Epic Issue Picker Field Object   
    
                         issueLink = customFieldManager.getCustomFieldObject(FEATURE_ISSUE_PICKER_ID); // Initiave Issue Picker To Feature;
                    
                    log.warn(issueLink);


def inputIssues = issue.getCustomFieldValue(issueLink) as Collection; // initiative value as an Array
log.warn(inputIssues);


if(!inputIssues) {
    removeIssueLink(issue);
    return;
}; // if issue picker value is empty - return;  =====> We dont want to run throught this array below if there is no value here;


def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager);
def availableIssueLinkTypes = issueLinkTypeManager.issueLinkTypes;

def issueLinkName = setIssueLinkNameByUpdatedIssueType(issue) as String; // Getting the issue type link name by a funtion written Below. will use the link type ID below.

def linkType = availableIssueLinkTypes.findByName(issueLinkName);

//
removeIssueLink(issue)

MutableIssue innerIssue = issueManager.getIssueObject(inputIssues);

innerIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(EPIC_ISSUE_PICKER_ID), issue);

linkManager.createIssueLink(issue.getId() as Long, innerIssue.getId() as Long, linkType.id as Long, 1 as Long, user); // Setting Jira's Main Linked Issues by givven value;

issueManager.updateIssue(user, innerIssue, EventDispatchOption.DO_NOT_DISPATCH, false);



private void removeIssueLink(Issue issue){
    
    def linkManager = ComponentAccessor.getIssueLinkManager();
    def issueManager = ComponentAccessor.getIssueManager();
    def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
    def outward = linkManager.getOutwardLinks(issue.getId()) as Collection <IssueLink>;

for(def i = 0; i < outward.size(); i++) {
    
 MutableIssue tempIssue = issueManager.getIssueObject(outward[i].sourceId);
 linkManager.removeIssueLink(outward[i], user);
}
}

private String setIssueLinkNameByUpdatedIssueType(Issue issue) {
    
    def issueLinkName;
    
 if(getIssueType(issue) == "Feature" ) {
        
                        issueLinkName = "Initiative-Features";
             }
        
return issueLinkName;

}


private String getIssueType(Issue issue){
    
    return issue.getIssueType()?.getName();
}
