import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.ApplicationUser;


// Globals
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser;
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def issueManager = ComponentAccessor.getIssueManager();
OptionsManager optManager = ComponentAccessor.getOptionsManager(); 

// Search service =========>
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService);
// Custom Fields ======>
def epicLinkField = customFieldManager.getCustomFieldObject("customfield_10100");
def scrumTeamField = customFieldManager.getCustomFieldObject("customfield_10201");
def sprintField = customFieldManager.getCustomFieldObject("customfield_10104");
// Issue pickers Fields =========>
def sprintPickerField = customFieldManager.getCustomFieldObject("customfield_14038");
def cyclePickerField = customFieldManager.getCustomFieldObject("customfield_14028");
def scrumTeamPickerField = customFieldManager.getCustomFieldObject("customfield_14053");
def scrumTeamPicker = customFieldManager.getCustomFieldObject("customfield_14081");

// Extracting the linked allocation to the current epic.
def query = jqlQueryParser.parseQuery("issuetype = 'Allocation' and 'Epic Link' = '${issue.key.toString()}'");
def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults();

MutableIssue allocationIssue;
// in case we find linked allocations :
if(results.size() > 0) {
    
    for(def currentAllocation = 0; currentAllocation < results.size(); currentAllocation ++ ) {

        allocationIssue = issueManager.getIssueObject(results[currentAllocation].key);
        
        handleIssueChanges(issue, allocationIssue,scrumTeamField, sprintField, scrumTeamPickerField ,sprintPickerField, cyclePickerField, scrumTeamPicker, issueManager, optManager);
        updateIssue(allocationIssue, user, issueManager);

	}

}

// Logic ========>


private Issue getRelatedIssueByIssuePIckerField(Issue issue, CustomField currentField,IssueManager issueManager) {
    Issue relatedIssue = issueManager.getIssueObject(issue.getCustomFieldValue(currentField).toString());
    return relatedIssue
}


private Option getScrumTeamValue (MutableIssue allocation, CustomField scrumTeamField, epicScrumTeam, OptionsManager optManager) {
    
	Options options = optManager.getOptions(scrumTeamField.getRelevantConfig(allocation));
	def fieldConfig = scrumTeamField.getRelevantConfig(allocation);
	def value = ComponentAccessor.optionsManager.getOptions(fieldConfig)?.find { it.toString().trim() == epicScrumTeam.toString().trim() } 
	return value;
}

private void updateIssue(MutableIssue issueToUpdate, ApplicationUser user, IssueManager issueManager) {
    
	issueManager.updateIssue(user, issueToUpdate, EventDispatchOption.ISSUE_UPDATED, false);
}


private void handleIssueChanges(Issue issue, MutableIssue allocationIssue, CustomField scrumTeamField, CustomField sprintField, CustomField scrumTeamPickerField, CustomField sprintPickerField, CustomField cyclePickerField, CustomField scrumTeamPicker, IssueManager issueManager, OptionsManager optManager) {
    def currentSprint = getRelatedIssueByIssuePIckerField(issue, sprintPickerField, issueManager) as Issue;
    def currentCycle = getRelatedIssueByIssuePIckerField(issue, cyclePickerField, issueManager) as Issue;
    def newScrumTeam = getRelatedIssueByIssuePIckerField(issue, scrumTeamPickerField, issueManager) as Issue;
    
   def scrumTeamOfScrumIssue = getScrumTeamValue(allocationIssue, scrumTeamField, newScrumTeam.getCustomFieldValue(scrumTeamField).toString(), optManager);
    
    
    allocationIssue.setCustomFieldValue(scrumTeamPicker, newScrumTeam ? newScrumTeam : null);

    allocationIssue.setCustomFieldValue(sprintPickerField, currentSprint ? currentSprint : null);

    allocationIssue.setCustomFieldValue(cyclePickerField, currentCycle ? currentCycle : null);
    
    allocationIssue.setCustomFieldValue(scrumTeamField, scrumTeamOfScrumIssue ? scrumTeamOfScrumIssue : null);
    
    allocationIssue.setSummary(newScrumTeam.getCustomFieldValue(scrumTeamField).toString());
}

        
