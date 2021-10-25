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
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Options;
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.customisers.WithPlugin;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.onresolve.jira.groovy.user.FieldBehaviours;
import groovy.transform.BaseScript;
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.atlassian.greenhopper.service.sprint.SprintManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.issue.customfields.option.Option
Issue issue = event.issue;

@BaseScript FieldBehaviours fieldBehaviours;

if (!isEpic(issue)) return;

// Managers and system vars.
def customFieldManager = ComponentAccessor.getCustomFieldManager();
def searchProvider = ComponentAccessor.getComponent(SearchProvider);
def user = ComponentAccessor.getJiraAuthenticationContext().getUser();
OptionsManager optManager = ComponentAccessor.getOptionsManager(); 
//script related vars.
def allocationIssueType = 11201
MutableIssue allocation;
def fieldChangesLocateByName = "Involved Scrum Team"

def epicLinkFieldId = "customfield_10100";
def scrumTeamField = customFieldManager.getCustomFieldObject("customfield_10201");

def involvedScrumTeams = getInvolvedScrumTeams(issue);

def sprintField = customFieldManager.getCustomFieldObject("customfield_10104");
def changeLog = event?.changeLog;
def changedItems = changeLog.getRelated("ChildChangeItem");
def allocationActionCheckbox = customFieldManager.getCustomFieldObject("customfield_14100");

// On Epic Edit Screen, Checkbox to check if agree Creating Allocations or don't. (Allocation Action)

    changedItems.find {
        
        if(it.field != fieldChangesLocateByName || issue.getCustomFieldValue(allocationActionCheckbox).toString() != '[Create Allocation for added Scrum teams]') return;
        
			def linkedAllocations = getAllLinkedAllocations(issue, user);

        		  for(def j = 0; j < involvedScrumTeams.size(); j ++) {      

                      if(linkedAllocations.toString().indexOf(involvedScrumTeams[j].toString()) == -1 && isScrumTeamExists(user, involvedScrumTeams[j])) {

                          // Setting up the new Allocaiton according to the involved scrum team field value;
                        allocation = ComponentAccessor.issueFactory.issue;
                          
                        allocation.projectObject = issue.getProjectObject();
                        allocation.issueTypeId = allocationIssueType;
                        allocation.summary = involvedScrumTeams[j];
                        allocation.setCustomFieldValue(scrumTeamField, setScrumTeamValue(allocation, scrumTeamField, involvedScrumTeams[j], optManager)); // Scrum Team.
                        allocation.setCustomFieldValue(customFieldManager.getCustomFieldObject(epicLinkFieldId) , issue); // Epic Link
                        allocation.setReporter(user);
                        !allocation.getCustomFieldValue(sprintField) ? allocation.setCustomFieldValue(sprintField, getActiveSprint(user)) : null; // Setting the sprint defaultly as active.
                        allocation.setAssignee(getAllocationAssignee(involvedScrumTeams[j], user));	
                        // Creating the new Allocation with all the configuration above.
                        ComponentAccessor.issueManager.createIssueObject(user, allocation);
                
                }      
            }
        }


// Setting the Jira native sprint field as the active sprint on the board, Defaultly.
private getActiveSprint (ApplicationUser user) {
@WithPlugin("com.pyxis.greenhopper.jira")
@JiraAgileBean
RapidViewService rapidViewService
@JiraAgileBean
SprintIssueService sprintIssueService
@JiraAgileBean
SprintManager sprintManager
    
    
    def sprintsToIssue = [] as Collection;

    def boardId = 287;
    def view = rapidViewService.getRapidView(user, boardId).getValue()
    def sprints = sprintManager.getSprintsForView(view).getValue()
    def activeSprint = sprints.find { it.active }
    
    if(activeSprint) {
        sprintsToIssue.add(activeSprint)
    	return sprintsToIssue
        
    }
    return null
}

// Getting the Scrum Team issue type & Getting the Team Focal Value, and using it to the Allocation's Summary.
private ApplicationUser getAllocationAssignee(currentIndexValue, ApplicationUser user) {
    
	def searchService = ComponentAccessor.getComponent(SearchService);
	def issueManager = ComponentAccessor.getIssueManager();
	def userSearchService = ComponentAccessor.getComponent(UserSearchService);
	def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
	def userManager = ComponentAccessor.getUserManager();
   
    def ALLOCATION_FOCAL_FIELD_ID = "customfield_14067";
    
	def teamFocal = customFieldManager.getCustomFieldObject(ALLOCATION_FOCAL_FIELD_ID);
    		def query = jqlQueryParser.parseQuery("issuetype = 'Scrum Team' and Summary ~ '${currentIndexValue}'");
			def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter())
			results = results.getResults();
                          
            if(results) {
                Issue scrumTeamIssue = issueManager.getIssueObject(results[0].key.toString());
                def teamFocalUser = scrumTeamIssue.getCustomFieldValue(teamFocal);
                // Setting the allocation's Assignee as the found scrum team's Focal user.
                if(teamFocalUser) {
                def seperator = teamFocalUser.toString().indexOf("(")
                def currentUser = teamFocalUser.toString().substring(0,seperator);
                ApplicationUser teamFocalU = userSearchService.findUsersByEmail(currentUser.toString())[0];

    	}
	}
}

private boolean isEpic(Issue issue) {
    return issue.getIssueType().getName() == "Epic"; 
}

private boolean isIndeciesSame(oldIndex, newIndex) {
    	return newIndex == oldIndex;
}

// Getting the allocation's Value to set it while running the script.
private Option setScrumTeamValue (MutableIssue allocation, CustomField scrumTeamField, newIndexValue, OptionsManager optManager) {
    
	Options options = optManager.getOptions(scrumTeamField.getRelevantConfig(allocation));
	def fieldConfig = scrumTeamField.getRelevantConfig(allocation);
	def value = ComponentAccessor.optionsManager.getOptions(fieldConfig)?.find { it.toString().trim() == newIndexValue.toString().trim() } 
	return value;
}


private Collection getInvolvedScrumTeams(Issue issue) {
def involvedScrumTeamFieldId = "customfield_11401";
def involvedScrumTeamdField = customFieldManager.getCustomFieldObject(involvedScrumTeamFieldId);
def involvedScrumTeams = issue.getCustomFieldValue(involvedScrumTeamdField) as Collection;
involvedScrumTeams = (!involvedScrumTeams || involvedScrumTeams.size() == 0) ? [] : involvedScrumTeams;

    return involvedScrumTeams
}


private Collection getAllLinkedAllocations(Issue issue, ApplicationUser user) {
    	def issueManager = ComponentAccessor.getIssueManager();
    	def searchService = ComponentAccessor.getComponent(SearchService);
		def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser);
    	MutableIssue allocationIssue;
    	def allocationsSummary = [];
    
        def query = jqlQueryParser.parseQuery("issuetype = Allocation and 'Epic Link' = '${issue.key.toString()}'");
		def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults();
    
        if(results) {
            for(def i = 0; i < results.size(); i++) {
                allocationIssue = issueManager.getIssueObject(results[i].key.toString());
                allocationsSummary.add(allocationIssue.getSummary());

                
            }
        }
    	return allocationsSummary;
}

private boolean isScrumTeamExists(ApplicationUser user, scrumTeamName) {
    def searchService = ComponentAccessor.getComponent(SearchService);
	def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser);
    
    def query = jqlQueryParser.parseQuery("issuetype = 'Scrum Team' and 'Summary' ~ '${scrumTeamName.toString()}'");
	def results = searchService.search(user,query, PagerFilter.getUnlimitedFilter()).getResults();
	
    if(results) {
        return true
    }
    return false
	}