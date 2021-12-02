import com.atlassian.jira.component.ComponentAccessor

import com.onresolve.jira.groovy.user.FieldBehaviours

import com.atlassian.jira.component.ComponentAccessor

import com.atlassian.jira.issue.Issue

import com.atlassian.jira.issue.customfields.option.Option

import com.atlassian.jira.issue.fields.CustomField

import com.atlassian.jira.user.ApplicationUser

import com.atlassian.jira.event.type.EventDispatchOption

import java.text.SimpleDateFormat;



def watcherManager = ComponentAccessor.getWatcherManager()

def userManager = ComponentAccessor.getUserManager()

def customFieldManager = ComponentAccessor.getCustomFieldManager()

def issueManager = ComponentAccessor.getIssueManager()

def issue = event.getIssue()

def mis = issueManager.getIssueObject(issue.key)



// Getting qaAssignee object

def qaAssignee = customFieldManager.getCustomFieldObject("customfield_12714")



// Getting qaAssignee field value

def qaAssigneeVal = (ApplicationUser)issue.getCustomFieldValue(qaAssignee)



def qaAssigneeUnassigned = userManager.getUserByName("Unassigned")



log.error('QaAssignee ' + qaAssigneeVal)



log.error('qaAssigneeUnassigned ' + qaAssigneeUnassigned)



if(qaAssigneeVal == null) {

mis.setCustomFieldValue(qaAssignee, qaAssigneeUnassigned)

issueManager.updateIssue(event.getUser(), mis, EventDispatchOption.ISSUE_UPDATED, false)

}

else

{

if(qaAssigneeVal.getName() != 'Unassigned')

watcherManager.startWatching(qaAssigneeVal, issue)

else

{

}

}