package org.nmcpye.datarun.jpa.datasubmission.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.Objects;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
@Builder
@Value
@Getter
public class SubmissionReferencesData {
    String assignmentId;
    String submissionId;
    String teamId;
    String teamCode;
    String orgUnitId;
    String templateId;
    String templateVersionId;
    String templateVersionNumber;
    String orgUnitCode;
    String orgUnitName;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SubmissionReferencesData that)) return false;
        return Objects.equals(getAssignmentId(), that.getAssignmentId()) && Objects.equals(getSubmissionId(), that.getSubmissionId()) && Objects.equals(getTeamId(), that.getTeamId()) && Objects.equals(getTeamCode(), that.getTeamCode()) && Objects.equals(getOrgUnitId(), that.getOrgUnitId()) && Objects.equals(getTemplateId(), that.getTemplateId()) && Objects.equals(getTemplateVersionId(), that.getTemplateVersionId()) && Objects.equals(getTemplateVersionNumber(), that.getTemplateVersionNumber()) && Objects.equals(getOrgUnitCode(), that.getOrgUnitCode()) && Objects.equals(getOrgUnitName(), that.getOrgUnitName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAssignmentId(), getSubmissionId(), getTeamId(), getTeamCode(), getOrgUnitId(), getTemplateId(), getTemplateVersionId(), getTemplateVersionNumber(), getOrgUnitCode(), getOrgUnitName());
    }
}
