import com.atlassian.jira.component.ComponentAccessor;
import com.onresolve.jira.groovy.user.FormField;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;

FormField assigneeField = getFieldByName("Assignee");
FormField teamFocal = getFieldByName("Team Allocation Focal");
FormField scrumTeamField = getFieldByName("Scrum Team");
FormField changeAssigneeCheckbox = getFieldByName("Change Assignee");
FormField relatedScrumTeamField = getFieldById("customfield_14053");

Issue scrumTeamIssue;

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService);
def issueManager = ComponentAccessor.getIssueManager();
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser() as ApplicationUser;
def userSearchService = ComponentAccessor.getComponent(UserSearchService)

def currentAssignee = userSearchService.findUsersByEmail(assigneeField.getValue().toString())

ApplicationUser userAssigned = currentAssignee[0];

def query = jqlQueryParser.parseQuery("issuetype = 'Scrum Team' AND 'Team Members' = '${userAssigned.getEmailAddress()}'");
def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults();

if(results.size() > 0) {
	    scrumTeamIssue = issueManager.getIssueObject(results[0].key.toString());
	    relatedScrumTeamField.setFormValue(scrumTeamIssue.toString())
    	scrumTeamField.setFormValue(scrumTeamIssue.getSummary())
}
