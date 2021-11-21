import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.greenhopper.service.sprint.SprintManager
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.customisers.WithPlugin;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.IssueManager;


FormField sprintPicker = getFieldByName("Related Sprint");
FormField sprintField = getFieldByName("Sprint");

def issueManager = ComponentAccessor.getIssueManager();
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

sprintPicker.setFormValue(getSprintIssueBySprintName(sprintField, user))


private String getSprintIssueBySprintName(FormField sprintField,ApplicationUser user ) { 
    FormField cyckePicker = getFieldByName("Related Cycle");

    @WithPlugin("com.pyxis.greenhopper.jira")
    @JiraAgileBean SprintManager sprintManager
   	CustomField cyclePickerCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("customfield_14028");

    IssueManager issueManager = ComponentAccessor.getIssueManager();

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def searchService = ComponentAccessor.getComponent(SearchService);

    def currentSprint = sprintField.getValue() as Long

    def sprintName = sprintManager.getSprint(currentSprint).get().name;
    def query = jqlQueryParser.parseQuery("issuetype = 'Sprint' and 'Sprint' = '${sprintName}'");
    
    def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults();

    if(results) {
        Issue sprintIssue = issueManager.getIssueObject(results[0].key);
       	cyckePicker.setFormValue(getCycleIssue(cyclePickerCF, sprintIssue, issueManager));
        return sprintIssue ? sprintIssue : null
    }
}


private String getCycleIssue(CustomField cyckePickerCF, Issue sprintIssue,IssueManager issueManager) {
    def cycleIssue = issueManager.getIssueObject(sprintIssue.getCustomFieldValue(cyckePickerCF).toString());
   return cycleIssue
    
}
