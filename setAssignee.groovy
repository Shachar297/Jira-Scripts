import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;

Issue issue = underlyingIssue;

def assignee = getFieldByName("Assignee").getValue();
def userSearchService = ComponentAccessor.getComponent(UserSearchService)
def user = userSearchService.findUsersByEmail(assignee.toString())
ApplicationUser userAssigned = user[0];
getFieldByName("Summary").setFormValue(userAssigned.getDisplayName());    
