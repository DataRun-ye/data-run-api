package org.nmcpye.datarun.assignmentsubmissionhistory;

import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.baseclass.MongoAuditableBaseObject;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "assignment_submission_history")
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssignmentSubmissionHistory
    extends MongoAuditableBaseObject {
    @Field("entries")
    private List<HistoryEntry> entries = new ArrayList<>();

    public static class HistoryEntry {
        @Field("submissionDate")
        private Instant submissionDate;

        @Field("entryDate")
        private Instant entryDate;

        @Field("submission")
        private String submission;

        @Field("assignedTeam")
        private String assignedTeam;

        @Field("reassignedTo")
        private String reassignedTo;

        @Field("rescheduledTo")
        private String rescheduledTo;

        @Field("mergedWith")
        private String mergedWith;

        @Field("cancelReason")
        private String cancelReason;

        @Field("deleted")
        private Boolean deleted;

        @Field("submissionTeam")
        private String submissionTeam;

        @Field("submissionUser")
        private String submissionUser;

        @Field("form")
        private String form;

        @Field("formVersion")
        private String formVersion;

        @Field("submissionStatus")
        private AssignmentStatus submissionStatus;

        @Field("allocatedResources")
        private Map<String, Object> allocatedResources;

        @Field("submittedResources")
        private Map<String, Double> submittedResources;

        public Instant getSubmissionDate() {
            return submissionDate;
        }

        public void setSubmissionDate(Instant submissionDate) {
            this.submissionDate = submissionDate;
        }

        public Instant getEntryDate() {
            return entryDate;
        }

        public void setEntryDate(Instant entryDate) {
            this.entryDate = entryDate;
        }

        public String getSubmission() {
            return submission;
        }

        public void setSubmission(String submission) {
            this.submission = submission;
        }

        public String getAssignedTeam() {
            return assignedTeam;
        }

        public void setAssignedTeam(String assignedTeam) {
            this.assignedTeam = assignedTeam;
        }

        public String getSubmissionUser() {
            return submissionUser;
        }

        public void setSubmissionUser(String submissionUser) {
            this.submissionUser = submissionUser;
        }

        public String getReassignedTo() {
            return reassignedTo;
        }

        public void setReassignedTo(String reassignedTo) {
            this.reassignedTo = reassignedTo;
        }

        public String getRescheduledTo() {
            return rescheduledTo;
        }

        public void setRescheduledTo(String rescheduledTo) {
            this.rescheduledTo = rescheduledTo;
        }

        public String getMergedWith() {
            return mergedWith;
        }

        public void setMergedWith(String mergedWith) {
            this.mergedWith = mergedWith;
        }

        public String getCancelReason() {
            return cancelReason;
        }

        public void setCancelReason(String cancelReason) {
            this.cancelReason = cancelReason;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

        public String getSubmissionTeam() {
            return submissionTeam;
        }

        public void setSubmissionTeam(String submissionTeam) {
            this.submissionTeam = submissionTeam;
        }

        public String getForm() {
            return form;
        }

        public void setForm(String form) {
            this.form = form;
        }

        public String getFormVersion() {
            return formVersion;
        }

        public void setFormVersion(String formVersion) {
            this.formVersion = formVersion;
        }

        public AssignmentStatus getSubmissionStatus() {
            return submissionStatus;
        }

        public void setSubmissionStatus(AssignmentStatus submissionStatus) {
            this.submissionStatus = submissionStatus;
        }

        public Map<String, Object> getAllocatedResources() {
            return allocatedResources;
        }

        public void setAllocatedResources(Map<String, Object> allocatedResources) {
            this.allocatedResources = allocatedResources;
        }

        public Map<String, Double> getSubmittedResources() {
            return submittedResources;
        }

        public void setSubmittedResources(Map<String, Double> submittedResources) {
            this.submittedResources = submittedResources;
        }
    }
}
