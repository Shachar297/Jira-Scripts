import com.onresolve.jira.groovy.user.FormField
import org.apache.log4j.Logger
import org.apache.log4j.Level;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.context.IssueContext;

//def selectedProject = ComponentAccessor.projectManager.getProjectObjByKey("AS2");

def log = Logger.getLogger(getClass())
log.setLevel(Level.DEBUG)
 


FormField cyclePicker = getFieldByName("Cycle Picker");
  log.debug("My Issue Picker Field Object = "+cyclePicker)

def pickerValue = cyclePicker.value as Collection;
//def currentJql = "issuetype = Feature and project = 'Allot Smart 2.0' and !issueFunction in hasLinks('Initiative') or issueFunction in linkedIssuesOf('key= " + issue.toString() + ',' + 'Initiative' + ")";

Issue issue = underlyingIssue;

def allotSmartTwo = "'Allot Smart 2.0'";
def allotMigration = "'Allot Smart Migration 2.0'"  

log.info(underlyingIssue);

def proj = underlyingIssue?.getProjectObject()

def key = underlyingIssue?.key;
def currentJql;
if(proj.name == allotSmartTwo) {
    currentJql = "issuetype = Cycle and project = ${allotSmartTwo}"
    
}else{
    currentJql = "issuetype = Cycle and project = ${allotMigration}"
    
}
cyclePicker.setConfigParam('currentJql' , currentJql);     
 
