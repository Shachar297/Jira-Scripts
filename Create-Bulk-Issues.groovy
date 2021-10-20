import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.atlassian.greenhopper.service.sprint.SprintManager
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.customisers.WithPlugin;

Issue issue = event.issue;

if (!isEpic(issue)) return;

// Managers and system vars.
def issueManager = ComponentAccessor.getIssueManager();
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def user = ComponentAccessor.getJiraAuthenticationContext().getUser();
//script related vars.
def allocationIssueType = 11201
MutableIssue allocation;
def fieldChangesLocateByName = "Involved Scrum Team"
def involvedScrumTeamFieldId = "customfield_11401";
def epicLinkFieldId = "customfield_10100"
def involvedScrumTeamdField = customFieldManager.getCustomFieldObject(involvedScrumTeamFieldId);
def involvedScrumTeams = issue.getCustomFieldValue(involvedScrumTeamdField) as Collection;
involvedScrumTeams = (!involvedScrumTeams || involvedScrumTeams.size() == 0) ? [] : involvedScrumTeams;
def changeLog = event?.changeLog;
def changedItems = changeLog.getRelated("ChildChangeItem");

if (!changedItems) return;

    changedItems.each {
        
        if(it.field != fieldChangesLocateByName) return;
        
            // Splitting the string to an array to iterate it by values to locate changes. =>

            def newValue = it.newstring.toString().split(",") as Collection;
                    
        		  for(def j = 0; j < newValue.size(); j ++) {            
            
            		for(def i = 0; i < involvedScrumTeams.size(); i ++) {
                      
                      if( !isIndeciesSame(newValue[j], involvedScrumTeams[i])) {
                          
                        allocation = ComponentAccessor.issueFactory.issue;
                        allocation.projectObject = issue.getProjectObject();
                        allocation.issueTypeId = allocationIssueType;
                        allocation.summary = newValue[j].toString();
                        allocation.setCustomFieldValue(customFieldManager.getCustomFieldObject(epicLinkFieldId) , issue); // Epic Link
                        ComponentAccessor.issueManager.createIssueObject(user, allocation);
                
                      }      
				}
            }
        }

private boolean isEpic(Issue issue) {
    return issue.getIssueType().getName() == "Epic"; 
}

private boolean isIndeciesSame(oldIndex, newIndex) {
    	return newIndex == oldIndex;
}
