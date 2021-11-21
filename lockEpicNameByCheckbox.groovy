import com.onresolve.jira.groovy.user.FormField

FormField checkBox = getFieldById("customfield_14108");
FormField epicName = getFieldByName("Epic Name");
FormField summary = getFieldByName("Summary");


epicName.setFormValue(!!checkBox.getValue() ? summary.getValue() : epicName.getValue()).setReadOnly(!!checkBox.getValue());

