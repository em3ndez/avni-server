package org.openchs.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.openchs.domain.SubjectType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "uuid"})
public class SubjectTypeContract extends ReferenceDataContract {

    @JsonProperty(value = "group")
    private boolean isGroup;

    @JsonProperty(value = "household")
    private boolean isHousehold;

    private Boolean active;

    private String type;

    private String subjectSummaryRule;


    public static SubjectTypeContract fromSubjectType(SubjectType subjectType) {
        SubjectTypeContract contract = new SubjectTypeContract();
        contract.setName(subjectType.getName());
        contract.setUuid(subjectType.getUuid());
        contract.setVoided(subjectType.isVoided());
        contract.setIsGroup(subjectType.isGroup());
        contract.setHousehold(subjectType.isHousehold());
        contract.setActive(subjectType.getActive());
        contract.setType(subjectType.getType().name());
        contract.setSubjectSummaryRule(subjectType.getSubjectSummaryRule());
        return contract;
    }

    public boolean isHousehold() {
        return isHousehold;
    }

    public void setHousehold(boolean household) {
        isHousehold = household;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setIsGroup(boolean group) {
        isGroup = group;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubjectSummaryRule() {
        return subjectSummaryRule;
    }

    public void setSubjectSummaryRule(String subjectSummaryRule) {
        this.subjectSummaryRule = subjectSummaryRule;
    }
}
