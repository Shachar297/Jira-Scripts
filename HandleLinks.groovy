Q
// The purpose of this Listener is to connect a work log issue to its parent issue and to the selected Placeholder.
// This Listener only Connect, Does not Disconnect. !
// Only runs when issue being created.


// CONSTS
def STORY_POINTS_FIELD_ID = "customfield_10106";
def PLACE_HOLDER_FIELD_ID = "customfield_14101";
def BUG_ISSUE_PICKER_FIELD_ID = "customfield_14105";
def BUG_KEY_FIELD = "customfield_14107";
def OUTWARD_LINK_NAME = "Log Work-Place Holder";
def INNER_LINK_NAME = "Log Work-Work Issue";
def LOG_WORK_ISSUE_TYPE_ID = 11401;
String placeHolderFieldValue = "";
int currentIssueStoryPoints = 0;
int placeHolderStoryPoints = 0;
double sumOfStoryPoints = 0;
Long linkId;

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
import groovy.transform.BaseScript;

@BaseScript FieldBehaviours fieldBehaviours;
Issue CurrentIssue = event.issue

if (!CurrentIssue.getIssueType().getName().equals("Work Log (SP)")) return // This Listener is relevant only to Work Log. if this is not Work Log Stop Now

// globals
def issueManager = ComponentAccessor.getIssueManager();
def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();

// Links 
// fields
def placeHolderField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(PLACE_HOLDER_FIELD_ID);
def storyPointsField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(STORY_POINTS_FIELD_ID);
def bugIssuePicker   = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(BUG_ISSUE_PICKER_FIELD_ID);
def bugKeyField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(BUG_KEY_FIELD);
// extracting the place holder issue. =>
placeHolderFieldValue = CurrentIssue.getCustomFieldValue(placeHolderField) ? CurrentIssue.getCustomFieldValue(placeHolderField) : ""; 
MutableIssue placeHolderIssue = issueManager.getIssueObject(placeHolderFieldValue);
MutableIssue bugIssue = issueManager.getIssueObject(CurrentIssue.getCustomFieldValue(bugKeyField).toString());
MutableIssue issue = issueManager.getIssueObject(CurrentIssue.key.toString());

// extracting the story point from each issue related.

// 14107
placeHolderStoryPoints = placeHolderIssue.getCustomFieldValue(storyPointsField) ? placeHolderIssue.getCustomFieldValue(storyPointsField) as int : 0; // Initializing the current Place holder's story points.
currentIssueStoryPoints = CurrentIssue.getCustomFieldValue(storyPointsField) ? CurrentIssue.getCustomFieldValue(storyPointsField) as int : 0;

// Calculation.
sumOfStoryPoints = (currentIssueStoryPoints - placeHolderStoryPoints) * -1;
placeHolderIssue.setCustomFieldValue(storyPointsField, sumOfStoryPoints);
updateIssue(placeHolderIssue, user, issueManager)

// Handling the links.
linkId = getLinkTypeId(OUTWARD_LINK_NAME)
linkIssues(CurrentIssue.getId(), placeHolderIssue.getId(), linkId, user)
// link Worklog to bug =>

linkId = getLinkTypeId(INNER_LINK_NAME);
linkIssues(CurrentIssue.getId(), bugIssue.getId(), linkId, user);

setAssigneeForCurrentIssue(issue);

updateIssue(issue, user, issueManager)
    
placeHolderIssue.setCustomFieldValue(bugIssuePicker, bugIssue);
updateIssue(placeHolderIssue, user, issueManager);

// functions =>
private Issue getBugIssue(Issue issue, ApplicationUser user) {
    
    def searchService = ComponentAccessor.getComponent(SearchService);
    def issueManager = ComponentAccessor.getIssueManager();
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    
    def query = jqlQueryParser.parseQuery("issuetype = \"bug\" and issueFunction in linkedIssuesOf('key= ${issue.getKey()}', \"Log Work-Work Issue\")");
    def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults()
    return;
   
}


private Long getLinkTypeId(String linkName) {
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager);
def availableIssueLinkTypes = issueLinkTypeManager.issueLinkTypes;
def linkType = availableIssueLinkTypes.findByName(linkName);
return linkType.id
}

private void linkIssues(currentIssueId, targetIssueId, linkTypeId, ApplicationUser user) {
def linkManager = ComponentAccessor.getIssueLinkManager();    
linkManager.createIssueLink(currentIssueId as Long, targetIssueId as Long, linkTypeId as Long, 0 as Long, user); // Setting Jira's Main Linked Issues by givven value;
    
}

private void updateIssue(MutableIssue issueToUpdate, ApplicationUser user, IssueManager issueManager) {
issueManager.updateIssue(user, issueToUpdate, EventDispatchOption.ISSUE_UPDATED, false);
    
}

private void setAssigneeForCurrentIssue(MutableIssue issue) {
    if(!issue.getAssignee() || issue.getAssignee() == null || issue.getAssignee() == "") {
    
    def assignee = issue.getReporter() as ApplicationUser;
    issue.setAssignee(assignee);
   }
}


