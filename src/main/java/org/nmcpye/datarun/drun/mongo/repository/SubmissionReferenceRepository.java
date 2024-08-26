package org.nmcpye.datarun.drun.mongo.repository;

import org.nmcpye.datarun.drun.mongo.domain.DataField;
import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.domain.ReferenceData;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.ValueType;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional
public class SubmissionReferenceRepository {

    final private MongoTemplate mongoTemplate;
    final TeamRelationalRepositoryCustom teamRepository;

    public SubmissionReferenceRepository(MongoTemplate mongoTemplate, TeamRelationalRepositoryCustom teamRepository) {
        this.mongoTemplate = mongoTemplate;
        this.teamRepository = teamRepository;
    }

    public ReferenceData getFormSubmissionReferenceProperty(DataField field, String team, Instant created) {
        List<String> submissionMainAttributes = List.of("id", "uid", "deleted", "start_entry_time",
            "finished_entry_time", "form", "activity", "createdBy",
            "createdDate", "lastModifiedBy", "lastModifiedDate");
        Instant twoMonthsAgo = Instant.now().minus(2, ChronoUnit.MONTHS);

        final String form = field.getReferenceInfo().getResourceId();

        Query query = new Query(Criteria.where("form").is(form).and("team").is(team)
            .and("createdDate").gte(created != null ? created : twoMonthsAgo));

        query.fields().include(submissionMainAttributes.toArray(new String[0]));

        final String resourceProperty = field.getReferenceInfo().getResourceProperty();
        if (resourceProperty != null) {
            query.fields().include(resourceProperty);
        }

        return new ReferenceData(form, field.getName(), mongoTemplate.find(query, DataFormSubmission.class));
    }

    public List<DataField> getReferenceFields(String activity) {
        Query query = new Query(Criteria.where("activity")
            .is(activity).and("fields.type").is(ValueType.Reference));

        List<DataForm> dataForms = mongoTemplate.find(query, DataForm.class);

        return dataForms.stream()
            .map(DataForm::getFields)
            .flatMap(Collection::stream)
            .filter((dataField) -> dataField.getType() == ValueType.Reference)
            .toList();
    }

    public Set<ReferenceData> getTeamReferenceData() {
        final List<Team> userTeams = teamRepository.findAllByUser();
        Set<ReferenceData> referenceData = new HashSet<>();
        for (final Team team : userTeams) {
            var dataFields = getReferenceFields(team.getActivity().getUid());
            referenceData
                .addAll(
                    dataFields.stream()
                        .map((f) ->
                            getFormSubmissionReferenceProperty(f, team.getUid(), null))
                        .collect(Collectors.toSet()));
        }
        return referenceData;
    }

}
