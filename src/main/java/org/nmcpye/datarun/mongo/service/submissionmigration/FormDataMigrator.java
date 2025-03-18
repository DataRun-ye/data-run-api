package org.nmcpye.datarun.mongo.service.submissionmigration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
import org.nmcpye.datarun.mongo.domain.DataFormSubmissionFinal;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.DefaultField;
import org.nmcpye.datarun.mongo.domain.datafield.Repeat;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.Map.entry;

@Component
@Transactional
public class FormDataMigrator {
    private static final Logger log = LoggerFactory.getLogger(TemplateDrivenMigrator.class);
    private final TeamRelationalRepositoryCustom teamRepository;
    private final AssignmentRelationalRepositoryCustom assignmentRepository;
    private final Map<String, String> fieldMappings; // Loaded from JSON
    private final Map<String, String> anotherPathMap; // Loaded from JSON
    private final Map<String, Object> defaultValuesMap; // Loaded

    public FormDataMigrator(TeamRelationalRepositoryCustom teamRepository, AssignmentRelationalRepositoryCustom assignmentRepository) {
        this.teamRepository = teamRepository;
        this.assignmentRepository = assignmentRepository;
        this.anotherPathMap = parseJsonMapping(anotherPath);
        this.defaultValuesMap = parseDefaultValues(defaultValues);
        this.fieldMappings = parseJsonMapping(jsonArrayString);
    }

    public DataFormSubmissionFinal migrateV1ToV2(DataFormSubmissionBu v1Submission, DataForm v2FormTemplate)
        throws Exception {
        final var submissionFinal = creatSubmissionFinal(v1Submission,
            UnmodifiableMap.unmodifiableMap(v1Submission.getFormData()), v2FormTemplate.getVersion());

        final Map<String, Object> formData = new LinkedHashMap<>(submissionFinal.getMetadata());
        formData.putAll(migrateFormData(v1Submission,
            UnmodifiableMap.unmodifiableMap(v1Submission.getFormData()), v2FormTemplate));

        submissionFinal.setFormData(formData);

        return submissionFinal;
    }

    private DataFormSubmissionFinal creatSubmissionFinal(DataFormSubmissionBu v1Submission,
                                                         Map<String, Object> formData, Integer version)
        throws RuntimeException {

        final DataFormSubmissionFinal submissionFinal = new DataFormSubmissionFinal();
        final List<String> submissionOrgUnit = JsonPath.read(formData, "$.mainSection..orgUnit");
        final List<String> status = JsonPath.read(formData, "$.mainSection..status");
        final List<String> workDay = JsonPath.read(formData, "$.mainSection..workDay");
        final String submissionTeamOld = (String) formData.get("_teamOld");
        final String submissionActivity = (String) formData.get("_activity");
        final String username = (String) formData.get("_username");

        final String teamOld = Objects.requireNonNullElse(v1Submission.getTeamOld(), submissionTeamOld);
        final String activity = Objects.requireNonNullElse(v1Submission.getActivity(), submissionActivity);

        Team team;
        try {
            var teams = teamRepository.findFirstByActivityAndUser(activity, username);
            if (teams.size() != 1) {
                throw new RuntimeException("more than one team or not found team of user:" + v1Submission.getUid());
            }

            team = teams.stream().findAny().get();
        } catch (Exception e) {
            log.error("Failed to fetch team setting old team: {}", v1Submission.getUid(), e);
            team = teamRepository.findByUid(teamOld)
                .orElseThrow(() ->
                    new NoSuchElementException("Not found team of old team: " + v1Submission.getUid()));
        }

        final Assignment assignment = assignmentRepository
            .findFirstByTeamAndOrgUnit(team.getUid(), submissionOrgUnit.stream().findFirst().get(), v1Submission.getActivity())
            .orElseThrow(() -> {
                log.error("Failed to fetch assignment: {}", v1Submission.getUid());
                return new NoSuchElementException("not found assignment: " + v1Submission.getUid());
            });

        final var orgUnit = assignment.getOrgUnit();

        Map<String, Object> metaMap =
            new LinkedHashMap<>(Map.ofEntries(
                entry("_submissionUid", v1Submission.getUid()),
                entry("_serialNumber", v1Submission.getSerialNumber()),
                entry("_deleted", v1Submission.getDeleted()),
                entry("_activity", activity),
                entry("_teamOld", v1Submission.getTeamOld()),
                entry("_assignment", assignment.getUid()),
                entry("_orgUnit", orgUnit.getUid()),
                entry("_orgUnitCode", orgUnit.getCode()),
                entry("_orgUnitName", orgUnit.getName()),
                entry("_team", team.getUid()),
                entry("_teamCode", team.getCode()),
                entry("_workDay", workDay.stream().findFirst().get()),
                entry("_status", status.stream().findFirst().get()),
                entry("_username", formData.get("_username")),
                entry("_version", version),
                entry("_form", v1Submission.getForm()),
                entry("_deviceId", formData.get("_deviceId"))
            ));

        submissionFinal.setId(v1Submission.getId());
        submissionFinal.setUid(v1Submission.getUid());
        submissionFinal.setSerialNumber(v1Submission.getSerialNumber());
        submissionFinal.setDeleted(v1Submission.getDeleted());
        submissionFinal.setActivity(activity);
        submissionFinal.setAssignment(assignment.getUid());
        submissionFinal.setOrgUnit(orgUnit.getUid());
        submissionFinal.setOrgUnitCode(orgUnit.getCode());
        submissionFinal.setOrgUnitName(orgUnit.getName());
        submissionFinal.setTeamOld(v1Submission.getTeamOld());
        submissionFinal.setTeam(team.getUid());
        submissionFinal.setTeamCode(team.getCode());
        submissionFinal.setWorkDay(workDay.stream().findFirst().get());
        submissionFinal.setStatus(AssignmentStatus.valueOf(status.stream().findFirst().get()));

        submissionFinal.setMetadata(metaMap);

        submissionFinal.setVersion(version);
        submissionFinal.setForm(v1Submission.getForm());
        submissionFinal.setFinishedEntryTime(v1Submission.getFinishedEntryTime());
        submissionFinal.setStartEntryTime(v1Submission.getStartEntryTime());
        submissionFinal.setCreatedBy(Objects.requireNonNullElse(v1Submission.getCreatedBy(), username));
        submissionFinal.setLastModifiedBy(v1Submission.getLastModifiedBy());

        return submissionFinal;

    }

    private Map<String, Object> migrateFormData(DataFormSubmissionBu v1Submission, Map<String, Object> formData, DataForm v2Template) {
        Objects.requireNonNull(v1Submission, "v1Submission cannot be null");
        Objects.requireNonNull(v2Template, "v2Template cannot be null");

        Map<String, Object> newFormData = new LinkedHashMap<>();
        migrateSection(v2Template.getFields(), formData, newFormData, v1Submission.getUid());
        return newFormData;
    }

    private void migrateSection(List<AbstractField> v2Fields, Map<String, Object> v1Data, Map<String, Object> v2Data, String submissionUid) {
        for (AbstractField field : v2Fields) {
            if (field instanceof Repeat repeatSection) {
                List<Map<String, Object>> repeats = processRepeats(repeatSection, v1Data, submissionUid);
                v2Data.put(field.getName(), repeats);
            } else if (field instanceof Section section) {
                Map<String, Object> nestedSection = new LinkedHashMap<>();
                migrateSection(section.getFields(), v1Data, nestedSection, submissionUid);
                v2Data.put(field.getName(), nestedSection);
            } else if (field instanceof DefaultField defaultField) {
                Object value = getMappedValue(v1Data, defaultField.getPath());
                if (value != null) {
                    v2Data.put(field.getName(), value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> processRepeats(Repeat repeatField, Map<String, Object> v1Data, String submissionUid) {
        String v1RepeatPath = fieldMappings.getOrDefault(repeatField.getPath(), repeatField.getPath());

        Object resolvedData = resolvePath(v1Data, v1RepeatPath);

        if (!(resolvedData instanceof List)) return Collections.emptyList();
        final List<Map<String, Object>> v1Repeats = (List<Map<String, Object>>) resolvedData;

        final List<Map<String, Object>> repeats = new ArrayList<>();
        for (Map<String, Object> v1Repeat : v1Repeats) {
            Map<String, Object> v2Repeat = new LinkedHashMap<>();
            migrateSection(repeatField.getFields(), v1Repeat, v2Repeat, submissionUid);


            final Map<String, ?> metaMap = UnmodifiableMap.unmodifiableMap(new LinkedHashMap<>(Map.ofEntries(
                entry("_id", v1Repeat.get("_id")),
                entry("_index", v1Repeat.get("_index")),
                entry("_parentId", submissionUid),
                entry("_submissionUid", submissionUid)
            )));

            v2Repeat.putAll(metaMap);
            repeats.add(v2Repeat);
        }
        return repeats;
    }

    private Object getMappedValue(Map<String, Object> v1Data, String currentVersionPath) {
        String v1Path = fieldMappings.getOrDefault(currentVersionPath, currentVersionPath);
        Object resolvedValue = resolvePath(v1Data, v1Path);
        if (resolvedValue != null) {
            return resolvedValue;
        } else if (anotherPathMap.get(currentVersionPath) != null) {
            return resolvePath(v1Data, anotherPathMap.get(currentVersionPath));
        }
        return defaultValuesMap.get(currentVersionPath);
    }

//    private <T> T resolvePath(Map<String, Object> data, String path) {
//        String jPath = "$." + path;
//        try {
//            return JsonPath.read(data, jPath);
//        } catch (Exception e) {
//            String vPath = anotherPathMap.get(path);
//            if (vPath != null) {
//                try {
//                    String aPath = "$." + vPath;
//                    return JsonPath.read(data, aPath);
//                } catch (Exception e2) {
//                    return null;
//                }
//
//            }
//            return null;
//        }
//    }

    // Utility to resolve nested paths like "distributionData.householdDataOld"
    private Object resolvePath(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (!(current instanceof Map)) return null;
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }


    static public Map<String, String> parseJsonMapping(String jsonArrayString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonArrayString, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse field mappings JSON", e);
        }
    }

    static public Map<String, Object> parseDefaultValues(String jsonArrayString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonArrayString, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse field mappings JSON", e);
        }
    }

    static String jsonArrayString = """
        {
          	"mainSection": "mainSection",
            "mainSection.workDay": "mainSection.locationStatus.workDay",
            "mainSection.status": "mainSection.locationStatus.status",
            "status_reassigned_details.targetedByOtherTeamName": "mainSection.locationStatus.targetedByOtherTeamName",
            "status_reassigned_details.targetedByOtherTeamNum": "mainSection.locationStatus.targetedByOtherTeamNum",
            "status_merged_details.mergedWithOtherVillage": "mainSection.locationStatus.mergedWithOtherVillage",
            "status_cancelled_details.notargetingReasons": "mainSection.locationStatus.notargetingReasons",
            "status_cancelled_details.otherReasonForNotargeting": "mainSection.locationStatus.otherReasonForNotargeting",
            "households_information.householdnames.householdHeadSerialNumber": "householdNumber",
            "households_information.householdnames.inputmethod": "inputmethod",
            "households_information.householdnames.householdName": "householdName",
            "households_information.householdnames.householdReference": "householdReference",
            "households_information.householdnames.population": "population",
            "households_information.householdnames.malePopulation": "malePopulation",
            "households_information.householdnames.femalePopulation": "femalePopulation",
            "households_information.householdnames.pregnantWomen": "pregnantWomen",
            "households_information.householdnames.childrenMaleCount": "childrenMaleCount",
            "households_information.householdnames.childrenFemaleCount": "childrenFemaleCount",
            "households_information.householdnames.displacedResidentsCount": "displacedResidentsCount",
            "households_information.householdnames.itns": "itns"
        }
        """;
    static String anotherPath = """
        {
            "households_information.householdnames.householdHeadSerialNumber": "householdHeadSerialNumber"
        }
        """;
    static String defaultValues = """
        {
            "mainSection.status": "DONE",
            "mainSection.locationStatus.status": "DONE",
            "completion_details.completionStatus": "FULL",
            "completion_details.completionPercentage": 100,
            "households_information.householdnames.inputmethod": "AddNew"
        }
        """;
}
