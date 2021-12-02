import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.Issue;
// --------------------------------------------------
// --------------------------------------------------
Issue issue = event.issue;
if(!isSubTask(issue)) return;

def selectedEvent = event.getChangeLog().getRelated("ChildChangeItem").find { it.field == "status" }
if(selectedEvent.oldstring != "In Progress" && selectedEvent.newstring != "Done") return;

// --------------------------------------------------
// --------------------------------------------------
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.link.IssueLink;
// --------------------------------------------------
// --------------------------------------------------
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser;
def linkManager = ComponentAccessor.getIssueLinkManager();
def issueManager = ComponentAccessor.getIssueManager();
MutableIssue storyIssue = issueManager.getIssueObject(linkManager.getInwardLinks(issue.id)[0].sourceId);
// --------------------------------------------------
// --------------------------------------------------
if(storyIssue.getStatus().getName().equals("In Progress")) {

    def linkedSubTasksToStory = linkManager.getOutwardLinks(storyIssue.id) as Collection <IssueLink>;
    def doneSubTasks = [];
    
    for (def currentSubTask = 0; currentSubTask < linkedSubTasksToStory.size(); currentSubTask ++ ) {

        MutableIssue subTaskIssue = issueManager.getIssueObject(linkedSubTasksToStory[currentSubTask].destinationId);

        if(subTaskIssue.getStatus().getName() == "Done") {

            doneSubTasks.push(subTaskIssue);
        }
    }
    
    // Were are getting into this script only if the updated sub-task moved to Done, 
    // We count all the Done subtasks - and then comparing the number of Done subtasks to the number of child subtasks of the parent story.
    // If the numbers are the same, that means this is the last subtasks that moved to Done => therefor, the story moves to Done aswell.
   
    if(doneSubTasks.size() == linkedSubTasksToStory.size()) {

        moveStoryToInDone(storyIssue, user, issueManager);
    }
}

// --------------------------------------------------
// --------------------------------------------------

private boolean isSubTask(Issue issue) {
    return issue.getIssueType().getName().equals("Sub-task");
}

private boolean isInProgress(Issue issue) {
    return issue.getStatus().getName().equals("In Progress");
}

private void moveStoryToInDone(MutableIssue storyIssue, ApplicationUser user, IssueManager issueManager) {
    WorkflowManager wf_manager = ComponentAccessor.getWorkflowManager();
    JiraWorkflow jira_wf = wf_manager.getWorkflow(storyIssue);
    IssueService issueService = ComponentAccessor.getIssueService();

    def actionId = 31;
    def transitionValidationResult = issueService.validateTransition(user, storyIssue.id, actionId, new IssueInputParametersImpl())
    def transitionResult = issueService.transition(user, transitionValidationResult);
    updateIssue(storyIssue,user, issueManager)
}

private void updateIssue(MutableIssue issueToUpdate, ApplicationUser user, IssueManager issueManager) {

    issueManager.updateIssue(user, issueToUpdate, EventDispatchOption.ISSUE_UPDATED, false);
}