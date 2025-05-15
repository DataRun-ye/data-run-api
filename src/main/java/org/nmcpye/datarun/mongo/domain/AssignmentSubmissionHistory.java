package org.nmcpye.datarun.mongo.domain;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.springframework.data.annotation.Id;
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

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    private String uid;

    @Field("entries")
    private List<HistoryEntry> entries = new ArrayList<>();

    @Setter
    @Getter
    public static class HistoryEntry {
        @Field("submissionDate")
        private Instant submissionDate;

        @Field("entryDate")
        private Instant entryDate;

        @Field("submission")
        private String submission;

        @Field("assignedTeam")
        private String assignedTeam;

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

    }
}
