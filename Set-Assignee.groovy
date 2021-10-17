import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.event.type.EventDispatchOption;

Issue issue = event.issue;

if(issue.getIssueType().getName() != "Allocation") return;

def issueManager = ComponentAccessor.getIssueManager();
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchProvider = ComponentAccessor.getComponent(SearchProvider);
def searchService = ComponentAccessor.getComponent(SearchService);
def userSearchService = ComponentAccessor.getComponent(UserSearchService)
def userManager = ComponentAccessor.getUserManager();
def user = ComponentAccessor.getJiraAuthenticationContext().getUser();
//

def ALLOCATION_FOCAL_FIELD_ID = "customfield_14067";
def teamFocal = customFieldManager.getCustomFieldObject(ALLOCATION_FOCAL_FIELD_ID);

def SCRUM_TEAM_FIELD_ID = "customfield_10201";
def scrumTeamField = customFieldManager.getCustomFieldObject(SCRUM_TEAM_FIELD_ID);

def query = jqlQueryParser.parseQuery("issuetype = 'Scrum Team' and Summary ~ '${issue.getCustomFieldValue(scrumTeamField)}'");
def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter())
results = results.getResults();


if(results) {
	Issue scrumTeamIssue = issueManager.getIssueObject(results[0].key.toString());
    def teamFocalUser = scrumTeamIssue.getCustomFieldValue(teamFocal);

    if(teamFocalUser) {
        def seperator = teamFocalUser.toString().indexOf("(")
        def currentUser = teamFocalUser.toString().substring(0,seperator);
		ApplicationUser teamFocalU = userSearchService.findUsersByEmail(currentUser.toString())[0];

        MutableIssue currentIssue = issueManager.getIssueObject(issue.key.toString())
        currentIssue.setAssignee(teamFocalU)
        
     issueManager.updateIssue(user, currentIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
    }
}