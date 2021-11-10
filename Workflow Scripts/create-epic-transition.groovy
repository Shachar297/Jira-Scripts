// Create Epic Transition

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueImpl;
import com.onresolve.jira.groovy.user.FormField;
import groovy.transform.BaseScript;
import com.onresolve.jira.groovy.user.FieldBehaviours;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.workflow.edit.Workflow;
import com.atlassian.jira.issue.status.Status;
import com.onresolve.jira.groovy.user.FieldBehaviours;
import com.onresolve.jira.groovy.user.FormField;
import groovy.transform.BaseScript;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.event.type.EventDispatchOption;

@BaseScript FieldBehaviours fieldBehaviours;


def project = ComponentAccessor.projectManager.getProjectObjByKey("AS2");
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser;
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
def currentEpicStatus = issue.getStatus();

// Epic Fields => 

def epicSummary = issue.summary;
def epicDescription = issue.description;

def issueService = ComponentAccessor.getIssueService();
// Setting up feature fields =>
MutableIssue allocation = ComponentAccessor.issueFactory.issue;

allocation.projectObject = project;
allocation.issueTypeId = 11201;
allocation.summary = epicSummary;
allocation.assignee = issue.getAssignee();
allocation.description = epicDescription;
allocation.reporter = issue.getReporter();
allocation.setCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_14023") , issue);

def linkManager = ComponentAccessor.getIssueLinkManager();

def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager);
def availableIssueLinkTypes = issueLinkTypeManager.issueLinkTypes;
def issueLinkName = "Allocation - Feature";
def linkType = availableIssueLinkTypes.findByName(issueLinkName)

    ComponentAccessor.issueManager.createIssueObject(user, allocation); // Creating Feature issue as object.
        
    //issue.setCustomFieldValue(epicIssueLinkedKey, feature.toString());
    log.warn(issue.toString());
    linkManager.createIssueLink(allocation.getId() as Long, issue.getId() as Long, linkType.id as Long, 1L as Long, user);
    
    MutableIssue feature = issueManager.getIssueObject(issue.id);

def values = issue.getCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_14024")) as Collection;

values = (!values || values.size() == 0) ? [] : values;

values.add(allocation)
    feature.setCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_14024") , values);

issueManager.updateIssue(user, feature, EventDispatchOption.DO_NOT_DISPATCH, false);




