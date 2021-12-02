import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.onresolve.jira.groovy.user.FieldBehaviours;
import com.atlassian.jira.issue.CustomFieldManager;

Issue issue = event.issue;


def issueManager = ComponentAccessor.getIssueManager();
def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser();
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def isFixVersionChangedOnEpicLevel = false;

def history = event?.changeLog.getRelated("ChildChangeItem").find{
        log.warn(it.field)
    
    if(it.field == "Fix Version") {
        isFixVersionChangedOnEpicLevel = true;
    }
};

if(!issue.getIssueType().name.equals("Epic") && !issue.getIssueType().name.equals("Story")) return
(issue.getIssueType().name.equals("Epic") && isFixVersionChangedOnEpicLevel) ? setStoryFixVersionsByEpic(issue, user, issueManager) : setStoryFixVersion(customFieldManager, issue, user, issueManager);
    

private void setStoryFixVersionsByEpic(Issue epicIssue, ApplicationUser user, IssueManager issueManager) {
    def searchService = ComponentAccessor.getComponent(SearchService);
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    
    MutableIssue storyIssue;
    
    def query = jqlQueryParser.parseQuery("issuetype = Story AND 'Epic Link' = ${epicIssue.getKey()}");
    def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults()
    if(results.size() > 0) {
        
        for(def currentStoryIssue = 0; currentStoryIssue < results.size(); currentStoryIssue ++ ) {
            storyIssue = issueManager.getIssueObject(results[currentStoryIssue].key);
            storyIssue.setFixVersions(epicIssue.getFixVersions());
            updateIssue(storyIssue, user, issueManager)
        }
    }
    
}

private void setStoryFixVersion(CustomFieldManager customFieldManager, Issue storyIssue, ApplicationUser user, IssueManager issueManager) {
    def epicLinkField = customFieldManager.getCustomFieldObject(10100);
    MutableIssue currentStoryIssue = issueManager.getIssueObject(storyIssue.key);
    if(storyIssue.getCustomFieldValue(epicLinkField)) {
		Issue epicIssue = issueManager.getIssueObject(currentStoryIssue.getCustomFieldValue(epicLinkField).toString());
        currentStoryIssue.setFixVersions(epicIssue.getFixVersions())
        updateIssue(currentStoryIssue, user, issueManager)
    }
    
}


private void updateIssue(MutableIssue issueToUpdate, ApplicationUser user, IssueManager issueManager) {
	issueManager.updateIssue(user, issueToUpdate, EventDispatchOption.ISSUE_UPDATED, false);
    
}