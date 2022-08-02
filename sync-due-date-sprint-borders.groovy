import com.onresolve.jira.groovy.user.FormField;
import java.text.SimpleDateFormat
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
// ---------------------------------------
// ---------------------------------------
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager();

FormField targetStart = getFieldById("customfield_10502");
FormField targetEnd = getFieldById("customfield_10503");
FormField dueDate = getFieldById("duedate");
FormField sprintPicker = getFieldByName("Sprint Picker");

CustomField targetStart_Cf = customFieldManager.getCustomFieldObject('customfield_12715');
CustomField targetEnd_Cf = customFieldManager.getCustomFieldObject('customfield_12716');

MutableIssue sprintIssue;
// ---------------------------------------
// ---------------------------------------
// We do not allow to choose a date which not in the 'border' date of the sprint issue start date and end date.

if(sprintPicker.getValue()) {
    sprintIssue = issueManager.getIssueObject(sprintPicker.getValue().toString());
}
// Checking if sprint issue is exists..
if(sprintIssue) {
    def dueDateValue;
    // We are getting the values as string. we need to translate them into date format.
    def sprintIssueTargetStart = new SimpleDateFormat("d/MMM/yy").format(sprintIssue.getCustomFieldValue(targetStart_Cf));
    def sprintIssueTargetEnd = new SimpleDateFormat("d/MMM/yy").format(sprintIssue.getCustomFieldValue(targetEnd_Cf));

    if(dueDate.getValue()) {

        dueDateValue = new SimpleDateFormat("d/MMM/yy").format(dueDate.getValue());

        if(
            (isDueDateStartsBeforeSprint(dueDate, dueDateValue,sprintIssueTargetStart) || 
             !isSameMonth(dueDate, dueDateValue, sprintIssueTargetStart, sprintIssueTargetEnd)) ||
            isDueDateEndsAfterSprint(dueDate, dueDateValue, sprintIssueTargetEnd) || 
            !isSameMonth(dueDate, dueDateValue, sprintIssueTargetStart, sprintIssueTargetEnd)) {
            // dueDate.setError("Please pick a date between the current Sprint's Date borders. <br> Start Date :<b> ${sprintIssueTargetStart} </b>, End Date :<b> ${sprintIssueTargetEnd} </b> " );
            targetStart.setFormValue(null);
            targetEnd.setFormValue(null);
            return null;

            // Checking only the date is not enough because it is parsed from a date.
            // Checking for Month (month is applied by a month name not a month number) (Jan, Not 1)...

        } else if(!isSameMonth(dueDate, dueDateValue, sprintIssueTargetStart, sprintIssueTargetEnd)) {
            // dueDate.setError("Please pick a date between the current Sprint's Date borders. <br> Start Date :<b> ${sprintIssueTargetStart} </b>, End Date :<b> ${sprintIssueTargetEnd} </b> " );
            targetStart.setFormValue(null);
            targetEnd.setFormValue(null);
            return null
        } else {
            // dueDate.clearError()
            targetStart.setFormValue(getDate(dueDate));
            targetEnd.setFormValue(getDate(dueDate));
            return null
        }
    }
}

private String getDate(FormField dueDate) {
    def newDate;
    if(dueDate.getValue()) {
        newDate = new SimpleDateFormat("d/MMM/yy").format(dueDate.getValue());
    }
    return newDate;
}

private boolean isSameMonth(FormField dueDate, String due, String sprintStartDate, String sprintEndDate) {
    return (due.split("/")[1] ==  sprintStartDate.split("/")[1]) || (due.split("/")[1] ==  sprintEndDate.split("/")[1])
}

private boolean isDueDateStartsBeforeSprint(FormField dueDate, String due, String sprintStartDate) {
    int dueStartDay = due.split("/")[0].toInteger();
    int sprintStartDay = sprintStartDate.split("/")[0].toInteger();
    return dueStartDay < sprintStartDay

}

private boolean isDueDateEndsAfterSprint(FormField dueDate, String due, String sprintEndDate) {
    int dueStartDay = due.split("/")[0].toInteger();
    int sprintStartDay = sprintEndDate.split("/")[0].toInteger();
    return dueStartDay < sprintStartDay

}
