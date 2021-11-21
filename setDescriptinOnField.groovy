// In the create screen of a work log issue, display the current related place holder's story points under field description.
// this only happends under the create issue screen.
import com.onresolve.jira.groovy.user.FormField
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;

if(getActionName() != "Create") return

Issue issue = underlyingIssue;

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager();

CustomField storyPoints = customFieldManager.getCustomFieldObject("customfield_10106")

FormField placeHolderPicker = getFieldByName("Place Holder Picker");
FormField workIssuePicker = getFieldById("customfield_14105");

if(placeHolderPicker.getValue()) {
    
	MutableIssue placeHolderIssue = issueManager.getIssueObject(placeHolderPicker.getValue().toString());
	MutableIssue workIssue = issueManager.getIssueObject(workIssuePicker.getValue().toString())
	   
	def placeHolderStoryPoints = placeHolderIssue.getCustomFieldValue(storyPoints);
	placeHolderPicker.setDescription("Selected ${placeHolderIssue.getIssueType().getName()} : ${placeHolderIssue} : has Remaining SP = ${placeHolderStoryPoints}");
	getFieldByName("Description").setFormValue("This Work Log was reported on Work done for \n\r <${workIssue.getIssueType().getName()} : ${workIssue.getKey()} : ${workIssue.getSummary()}> \n\r On the account of \n\r <${placeHolderIssue.getIssueType().getName()} : ${placeHolderIssue.getKey()} : ${placeHolderIssue.getSummary()}>").setReadOnly(true);

}else{
    placeHolderPicker.setDescription("")
}


