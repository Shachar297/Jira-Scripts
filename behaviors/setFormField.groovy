import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.greenhopper.service.sprint.SprintManager
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueManager

FormField sprintPicker = getFieldByName('Related Sprint')
FormField sprintField = getFieldByName('Sprint')

def issueManager = ComponentAccessor.getIssueManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

Issue sprintIssue = issueManager.getIssueObject(getSprintIssueBySprintName(sprintField, user).toString())
sprintPicker.setFormValue(sprintIssue)

private MutableIssue getSprintIssueBySprintName(FormField sprintField, ApplicationUser user ) {
    FormField cyckePicker = getFieldByName('Related Cycle')

    @WithPlugin('com.pyxis.greenhopper.jira')
    @JiraAgileBean SprintManager sprintManager

    def issueManager = ComponentAccessor.getIssueManager()

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def searchService = ComponentAccessor.getComponent(SearchService)

    def currentSprint = sprintField.getValue() as Long

    def sprintName = sprintManager.getSprint(currentSprint).get().name
    def query = jqlQueryParser.parseQuery("issuetype = 'Sprint' and 'Sprint' = '${sprintName}'")

    def results = searchService.search(user, query, PagerFilter.getUnlimitedFilter()).getResults()

    if (results) {
        MutableIssue sprintIssue = issueManager.getIssueObject(results[0].key)
        cyckePicker.setFormValue(getCycleIssue(cyclePickerCF, sprintIssue, issueManager))
        return sprintIssue ? sprintIssue : null
    }
}

private MutableIssue getCycleIssue(CustomField cyckePickerCF, Issue sprintIssue, IssueManager issueManager) {
    MutableIssue cycleIssue = issueManager.getIssueObject(sprintIssue.getCustomFieldValue(cyckePickerCF).toString())
    return cycleIssue
}
