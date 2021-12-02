
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

if(getIssueType(issue) != "Feature" || getIssueType(issue) != "Allocation") return; // this script only handles Allocation < === > Feature Side to Side issue pickers and linking the both issue types

// CONSTS Declerations =>
def RELATED_ALLOCATIONS_FIELD = "customfield_14024";
def RELATED_FEATURE_FIELD = "customfield_14023"

// Global Declerations =>

def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
def linkManager = ComponentAccessor.getIssueLinkManager();
def issueManager = ComponentAccessor.getIssueManager();
def issueLink;
def inputIssues;
def currentValueForInnerIssuePicker
MutableIssue innerIssue;


if(getIssueType(issue) == "Allocation"){
    issueLink = customFieldManager.getCustomFieldObject(RELATED_FEATURE_FIELD); // Initiave Issue Picker To Feature;
    inputIssues = issue.getCustomFieldValue(issueLink) as String; // initiative value as an Array
    
} else if(getIssueType(issue) == "Feature"){  
    
                         issueLink = customFieldManager.getCustomFieldObject(RELATED_ALLOCATIONS_FIELD); // Initiave Issue Picker To Feature;
                         inputIssues = issue.getCustomFieldValue(issueLink) as Collection; // initiative value as an Array
                    
}else return;


if(!inputIssues || inputIssues.size() == 0) return; // if issue picker value is empty - return;  =====> We dont want to run throught this array below if there is no value here;

// setting linking types => 


def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager);
def availableIssueLinkTypes = issueLinkTypeManager.issueLinkTypes;

def issueLinkName = setIssueLinkNameByUpdatedIssueType(issue) as String; // Getting the issue type link name by a funtion written Below. will use the link type ID below.

def linkType = availableIssueLinkTypes.findByName(issueLinkName);

def outward;
if(getIssueType(issue) == "Feature") {
        outward = linkManager.getInwardLinks(issue.getId()) as Collection <IssueLink>;

}else{
    outward = linkManager.getOutwardLinks(issue.getId()) as Collection <IssueLink>;
}

MutableIssue tempIssue;
for(def i = 0; i < outward.size(); i++) {
           tempIssue = issueManager.getIssueObject(outward[i].sourceId);
    for(def j = 0; j < inputIssues.size(); j++) {

        if(tempIssue.toString() != inputIssues[j].toString()) {                     
            linkManager.removeIssueLink(outward[i], user);
        }
      }
    

  }


if(getIssueType(issue) == "Allocation") {
    

innerIssue = issueManager.getIssueObject(inputIssues);

currentValueForInnerIssuePicker = innerIssue.getCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_ALLOCATIONS_FIELD)) as Collection;

    if(!currentValueForInnerIssuePicker || currentValueForInnerIssuePicker.size() == 0) {
        
        currentValueForInnerIssuePicker = [issue];
    
            }else{
            
            currentValueForInnerIssuePicker.add(issue);    
        }
            //removeIssueLink(issue, inputIssues);
            
            innerIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_ALLOCATIONS_FIELD), currentValueForInnerIssuePicker);
            createIssueLinkAndUpdateIssue(issue, innerIssue, linkType.id);
    
    }    else if(getIssueType(issue) == "Feature") {
    
        for(def currentEpicChild = 0;  currentEpicChild < inputIssues.size(); currentEpicChild ++) {
            
            innerIssue = issueManager.getIssueObject(inputIssues[currentEpicChild].toString());
            log.warn(innerIssue);
            innerIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_FEATURE_FIELD), issue);
            createIssueLinkAndUpdateIssue(issue, innerIssue, linkType.id);
        }
}

// Case when a value is removed from the issue picker we want to indicate which one was it, remove it from the related issue picker, and UN-LINK it.
    for(def i = 0; i < outward.size(); i++) {
           tempIssue = issueManager.getIssueObject(outward[i].sourceId);
    for(def j = 0; j < inputIssues.size(); j++) {

        if(tempIssue.toString() != inputIssues[j].toString()) {                     
            linkManager.removeIssueLink(outward[i], user);
            tempIssue.setCustomFieldValue(customFieldManager.getCustomFieldObject(RELATED_FEATURE_FIELD), null)
            issueManager.updateIssue(user, tempIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
            
        }
      }
    
    
  }

// Useable functions => 


private void createIssueLinkAndUpdateIssue(Issue issue, MutableIssue innerIssue, linkTypeId) {
        def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
        def linkManager = ComponentAccessor.getIssueLinkManager();
        def issueManager = ComponentAccessor.getIssueManager();
           
    log.warn(issue.id)
    log.warn(innerIssue.id);
    log.warn(linkTypeId);
        
    if(getIssueType(issue) == "Allocation") {
        linkManager.createIssueLink(issue.getId() as Long, innerIssue.getId() as Long, linkTypeId as Long, 1 as Long, user); // Setting Jira's Main Linked Issues by givven value;
        
    }else{
        linkManager.createIssueLink(innerIssue.getId() as Long, issue.getId() as Long, linkTypeId as Long, 1 as Long, user); // Setting Jira's Main Linked Issues by givven value;
        
    }

        issueManager.updateIssue(user, innerIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
    
}


private String setIssueLinkNameByUpdatedIssueType(Issue issue) {
    
    def issueLinkName;
    
    issueLinkName = "Allocation - Feature";
        
return issueLinkName;

}


private String getIssueType(Issue issue){
    
    return issue.getIssueType()?.getName();
}