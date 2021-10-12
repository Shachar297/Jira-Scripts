import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.Issue;
import com.opensymphony.workflow.InvalidInputException;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.ImmutableCustomField;
import com.atlassian.greenhopper.service.sprint.Sprint;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.bc.issue.search.SearchService;

Issue issue = issue;

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchProvider = ComponentAccessor.getComponent(SearchProvider);
def searchService = ComponentAccessor.getComponent(SearchService);
def issueManager = ComponentAccessor.getIssueManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getUser();
def changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
def jiraSprint = issue.getSummary();
    //ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Summary");
//def sprint = issue.getCustomFieldValue(jiraSprint); 
log.warn(jiraSprint)


def query = jqlQueryParser.parseQuery("issuetype = Sprint and Sprint = '${jiraSprint.toString()}'");

def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter())
log.warn(results);
results = results.getResults()
log.warn(results);
if(!results || results.size() == 0) {
    return true;
}else{
    throw new InvalidInputException("Can Not Add Sprint ${jiraSprint}, That already exists in the system.")
    return false;
}


