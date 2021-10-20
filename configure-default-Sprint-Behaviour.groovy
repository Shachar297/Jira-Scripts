import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.CustomFieldManager;
import com.onresolve.jira.groovy.user.FieldBehaviours;
import com.onresolve.jira.groovy.user.FormField;
import groovy.transform.BaseScript;
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.atlassian.greenhopper.service.sprint.SprintManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.customisers.WithPlugin;

@BaseScript FieldBehaviours fieldBehaviours;

@WithPlugin("com.pyxis.greenhopper.jira")

@JiraAgileBean
RapidViewService rapidViewService

@JiraAgileBean
SprintIssueService sprintIssueService

@JiraAgileBean
SprintManager sprintManager

def boardId = 287;
def loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def view = rapidViewService.getRapidView(loggedInUser, boardId).getValue()

FormField sprintField = getFieldByName("Sprint");

def sprints = sprintManager.getSprintsForView(view).getValue()



def activeSprint = sprints.find { it.active }

if (activeSprint && !sprintField.getValue()) {
sprintField.setFormValue(activeSprint.id);
}
