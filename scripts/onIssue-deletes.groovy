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
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.index.IssueIndexingService

// Globals
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser;
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def issueManager = ComponentAccessor.getIssueManager();
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService);
def issueIndexingService = ComponentAccessor.getComponent(IssueIndexingService);

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
            allocationIssue.setPriority(issue.getPriority());
        }		

        if(isAllocationActivated(allocationIssue)) {

            Issue scrumTeamIssue = getScrumTeamIssue(allocationIssue, jqlQueryParser, searchService, issueManager, user, scrumTeamField);

            ApplicationUser teamFocalUser = scrumTeamIssue.getCustomFieldValue(teamFocal) as ApplicationUser;

            // Set Allocation Pickers from epic pickers if 
            if(isSettingAllocationPickersValues(issue, checkbox)) { 

                //setAllocationPickersFromEpic(issue, allocationIssue, sprintPickerField, cyclePickerField, issueManager);
            }
            
            if(teamFocalUser && !isAllocationHasAssignee(allocationIssue)) {

                allocationIssue.setAssignee(teamFocalUser);

                if(isSettingAllocationPickersValues(issue, checkbox)) {
                    //handleIssueChanges(issue, allocationIssue, sprintField, sprintPickerField, cyclePickerField, issueManager);                
                }

                updateIssue(allocationIssue, user, issueManager); 


            }


            //setAllocationPriority(issue, allocationIssue);	
            issueManager.updateIssue(user, allocationIssue, EventDispatchOption.ISSUE_UPDATED, false);

        }
        //setAllocationPickersFromEpic(issue, allocationIssue, sprintPickerField, cyclePickerField, issueManager);

        updateIssue(allocationIssue, user, issueManager);
        issueIndexingService.reIndex(allocationIssue);  

    }

}
// Logic ========>

private void updateIssue(MutableIssue issueToUpdate, ApplicationUser user, IssueManager issueManager) {

    issueManager.updateIssue(user, issueToUpdate, EventDispatchOption.ISSUE_UPDATED, false);
}

private void setAllocationPriority(Issue issue, MutableIssue allocationIssue) {
    allocationIssue.setPriority(issue.getPriority());

}

private void setAllocationPickersFromEpic(Issue epicIssue, MutableIssue allocationIssue, CustomField sprintPickerField, CustomField cyclePickerField, IssueManager issueManager) {

    if(epicIssue.getCustomFieldValue(sprintPickerField)) {
        Issue sprintIssue = issueManager.getIssueObject(epicIssue.getCustomFieldValue(sprintPickerField).toString());
        if(sprintIssue) {
            allocationIssue.setCustomFieldValue(sprintPickerField, sprintIssue)

        }
    } else {
        allocationIssue.setCustomFieldValue(sprintPickerField, null)

    }

    if(epicIssue.getCustomFieldValue(cyclePickerField)) {
        Issue cycleIssue = issueManager.getIssueObject(epicIssue.getCustomFieldValue(cyclePickerField).toString());
        if(cycleIssue) {
            allocationIssue.setCustomFieldValue(cyclePickerField, cycleIssue)

        }
    }else {
        allocationIssue.setCustomFieldValue(cyclePickerField, null)

    }

}

private void activateAllocation(MutableIssue allocationIssue, ApplicationUser user, IssueService issueService, IssueManager issueManager) {

    WorkflowManager wf_manager = ComponentAccessor.getWorkflowManager();
    JiraWorkflow jira_wf = wf_manager.getWorkflow(allocationIssue);

    def actionId = 71;

    def transitionValidationResult = issueService.validateTransition(user, allocationIssue.id, actionId, new IssueInputParametersImpl())
    def transitionResult = issueService.transition(user, transitionValidationResult);


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

    if(isValueExists(allocationIssue, sprintPickerField)) {
        def currentSprint = getRelatedIssueByIssuePIckerField(issue, sprintPickerField, issueManager) as Issue;
        allocationIssue.setCustomFieldValue(sprintPickerField, currentSprint ? currentSprint : null);
    }


    if(isValueExists(allocationIssue, cyclePickerField)) {

        def currentCycle = getRelatedIssueByIssuePIckerField(issue, cyclePickerField, issueManager) as Issue;
        allocationIssue.setCustomFieldValue(cyclePickerField, currentCycle ? currentCycle : null);
    }


}

private boolean isAllocationActivated(MutableIssue allocationIssue) {
    return !(allocationIssue.getStatus().getName().equals("New") || allocationIssue.getStatus().getName().equals("Cancelled"));
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


private boolean isSettingAllocationPickersValues(Issue issue, CustomField checkBox) {
    return !!issue.getCustomFieldValue(checkBox);
}