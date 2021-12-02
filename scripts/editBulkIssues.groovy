import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.Issue;


Issue issue = event.issue;

if(!isEpic(issue)) return;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.JiraWorkflow;


// Globals
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser;
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def issueManager = ComponentAccessor.getIssueManager();
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService);
IssueService issueService = ComponentAccessor.getIssueService();

def checkbox = customFieldManager.getCustomFieldObject("customfield_14200");
def activateAllocationsCheckbox = customFieldManager.getCustomFieldObject("customfield_14202");
def sprintField = customFieldManager.getCustomFieldObject("customfield_10104");
def sprintPickerField = customFieldManager.getCustomFieldObject("customfield_14038");
def cyclePickerField = customFieldManager.getCustomFieldObject("customfield_14028");
def scrumTeamField = customFieldManager.getCustomFieldObject("customfield_10201");
def teamFocal = customFieldManager.getCustomFieldObject("customfield_14067");

// Extracting the linked allocation to the current epic.
def query = jqlQueryParser.parseQuery("issuetype = 'Allocation' and 'Epic Link' = '${issue.key.toString()}'");
def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults();

MutableIssue allocationIssue;

if(results.size() > 0) {

    for(def currentAllocation = 0; currentAllocation < results.size(); currentAllocation ++ ) {

        allocationIssue = issueManager.getIssueObject(results[currentAllocation].key);


        if(isActivatingAllocation(issue, activateAllocationsCheckbox) && !isAllocationActivated(allocationIssue)) {

            activateAllocation(allocationIssue,user, issueService, issueManager);

        }		

        if(isAllocationActivated(allocationIssue)) {


            handleIssueChanges(issue, allocationIssue, sprintField, sprintPickerField, cyclePickerField, issueManager);

            Issue scrumTeamIssue = getScrumTeamIssue(allocationIssue, jqlQueryParser, searchService, issueManager, user, scrumTeamField);
				log.warn(scrumTeamIssue)
            ApplicationUser teamFocalUser = scrumTeamIssue.getCustomFieldValue(teamFocal) as ApplicationUser;
				log.warn(teamFocalUser)
            if(teamFocalUser && !isAllocationHasAssignee(allocationIssue)) {

                allocationIssue.setAssignee(teamFocalUser);     
            }

            updateIssue(allocationIssue, user, issueManager);
        }

    }

}
// Logic ========>

private void updateIssue(MutableIssue issueToUpdate, ApplicationUser user, IssueManager issueManager) {

    issueManager.updateIssue(user, issueToUpdate, EventDispatchOption.ISSUE_UPDATED, false);
}

private void activateAllocation(MutableIssue allocationIssue, ApplicationUser user, IssueService issueService, IssueManager issueManager) {

    WorkflowManager wf_manager = ComponentAccessor.getWorkflowManager();
    JiraWorkflow jira_wf = wf_manager.getWorkflow(allocationIssue);

    def actionId = 71;

    def transitionValidationResult = issueService.validateTransition(user, allocationIssue.id, actionId, new IssueInputParametersImpl())
    def transitionResult = issueService.transition(user, transitionValidationResult);

    updateIssue(allocationIssue, user, issueManager);

}


private Issue getScrumTeamIssue(MutableIssue issue, JqlQueryParser jqlQueryParser, SearchService searchService, IssueManager issueManager, ApplicationUser user, CustomField scrumTeamField) {
    def query = jqlQueryParser.parseQuery("issuetype = 'Scrum Team' and 'Scrum Team' = '${issue.getCustomFieldValue(scrumTeamField)}'");
    def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter())
    results = results.getResults();


    if(results) {
        Issue scrumTeamIssue = issueManager.getIssueObject(results[0].key.toString());
        return scrumTeamIssue
    }
    return null
}

private Issue getRelatedIssueByIssuePIckerField(Issue issue, CustomField currentField,IssueManager issueManager) {
    Issue relatedIssue = issueManager.getIssueObject(issue.getCustomFieldValue(currentField).toString());
    return relatedIssue
}

private void handleIssueChanges(Issue issue, MutableIssue allocationIssue, CustomField sprintField, CustomField sprintPickerField, CustomField cyclePickerField, IssueManager issueManager) {

    if(!isValueExists(allocationIssue, sprintPickerField)) {
        def currentSprint = getRelatedIssueByIssuePIckerField(issue, sprintPickerField, issueManager) as Issue;
        allocationIssue.setCustomFieldValue(sprintPickerField, currentSprint ? currentSprint : null);
    }


    if(isValueExists(allocationIssue, cyclePickerField)) {

        def currentCycle = getRelatedIssueByIssuePIckerField(issue, cyclePickerField, issueManager) as Issue;
        allocationIssue.setCustomFieldValue(cyclePickerField, currentCycle ? currentCycle : null);
    }


}

private boolean isAllocationActivated(MutableIssue allocationIssue) {
    return !allocationIssue.getStatus().getName().equals("New") && !allocationIssue.getStatus().getName().equals("Cancelled");
}

private boolean isActivatingAllocation(Issue epicIssue, CustomField activateAllocationsCheckbox) {
    return epicIssue.getCustomFieldValue(activateAllocationsCheckbox);
}


private boolean isEpic(Issue issue) {
    return issue.getIssueType().getName().equals("Epic");
}

private boolean isValueExists(MutableIssue allocationIssue, CustomField currentField) {
    return allocationIssue.getCustomFieldValue(currentField);
}

private boolean isAllocationHasAssignee(MutableIssue allocationIssue) {
    return allocationIssue.getAssignee()
}