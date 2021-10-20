import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.atlassian.greenhopper.service.sprint.SprintManager
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.customisers.WithPlugin;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.fields.CustomField;

@BaseScript FieldBehaviours fieldBehaviours;
@WithPlugin("com.pyxis.greenhopper.jira")
@JiraAgileBean
RapidViewService rapidViewService
@JiraAgileBean
SprintIssueService sprintIssueService
@JiraAgileBean
SprintManager sprintManager
//     - Managers -
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
//    - Sprint Related -
Issue issue = event.issue;

def sprintsToIssue = [] as Collection;

def boardId = 287;
def view = rapidViewService.getRapidView(loggedInUser, boardId).getValue()
def sprintField = customFieldManager.getCustomFieldObject("customfield_10104");
def sprints = sprintManager.getSprintsForView(view).getValue()
def activeSprint = sprints.find { it.active }


if (activeSprint && !issue.getCustomFieldValue(sprintField)) {
	handleIssueChanges(issue, activeSprint.id, sprintsToIssue, sprintField);
}

private void handleIssueChanges (Issue issue, Long sprintId, Collection issueSprints, CustomField sprintField) {
	def loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
	def issueManager = ComponentAccessor.getIssueManager();
	// 
    	sprintsToIssue.add(activeSprint)
    	MutableIssue currentIssue = issueManager.getIssueObject(issue.key.toString());
	currentIssue.setCustomFieldValue(sprintField, issueSprints);
    	issueManager.updateIssue(loggedInUser, currentIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
}
