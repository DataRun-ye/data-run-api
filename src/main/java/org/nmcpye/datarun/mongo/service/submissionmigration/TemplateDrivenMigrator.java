package org.nmcpye.datarun.mongo.service.submissionmigration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.bulk.BulkWriteResult;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Map.entry;

//@Component
//@Transactional
public class TemplateDrivenMigrator implements CommandLineRunner {
    // Add these constants at the class level
    private static final int BATCH_SIZE = 1000; // Adjust based on document size
    private static final int LOG_INTERVAL = 500; // Progress logging frequency
    private static final Logger log = LoggerFactory.getLogger(TemplateDrivenMigrator.class);
    private final Map<String, String> fieldMappings; // Loaded from JSON
    private final Map<String, String> anotherPathMap; // Loaded from JSON
    private final Map<String, Object> defaultValuesMap; // Loaded from JSON
    private final TeamRelationalRepositoryCustom teamRepository;
    private final AssignmentRelationalRepositoryCustom assignmentRepository;
    private final OrgUnitRelationalRepositoryCustom orgUnitRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper; // Reuse for performance

    public TemplateDrivenMigrator(TeamRelationalRepositoryCustom teamRepository,
                                  AssignmentRelationalRepositoryCustom assignmentRepository, OrgUnitRelationalRepositoryCustom orgUnitRepository,
                                  MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.orgUnitRepository = orgUnitRepository;
        this.objectMapper = objectMapper;
        anotherPathMap = parseJsonMapping(anotherPath);
        this.defaultValuesMap = parseDefaultValues(defaultValues);
        this.fieldMappings = parseJsonMapping(jsonArrayString);
        this.teamRepository = teamRepository;
        this.assignmentRepository = assignmentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    static public Map<String, String> parseJsonMapping(String jsonArrayString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonArrayString, new TypeReference<Map<String, String>>() {
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

    @Override
    public void run(String... args) throws Exception {
        runMainStream();
    }

    private void runErrorStream() {
        Query formTemplateQuery = Query.query(Criteria.where("uid").is("Tcf3Ks9ZRpB"));
        DataForm formTemplate = mongoTemplate.findOne(formTemplateQuery, DataForm.class);

        List<DataFormSubmissionBu> errorSubmissions = mongoTemplate.findAll(DataFormSubmissionBu.class, "data_form_submission_errors");
        final var errorsUid = errorSubmissions.stream().map(DataFormSubmissionBu::getUid).toList();

        Query query = Query.query(Criteria.where("uid").in(errorsUid));

        try (Stream<DataFormSubmissionBu> stream =
                 mongoTemplate.stream(query, DataFormSubmissionBu.class,
                     "data_form_submission_bu")) {

            stream.forEach(v1Submission -> {
                try {
                    processAssignmentAndTeamSubmission(v1Submission);
                    migrate(v1Submission, formTemplate);
                    DataFormSubmissionFinal submissionFinal = new DataFormSubmissionFinal();

                    submissionFinal.setId(v1Submission.getId());
                    submissionFinal.setUid(v1Submission.getUid());
                    submissionFinal.setSerialNumber(v1Submission.getSerialNumber());
                    submissionFinal.setDeleted(v1Submission.getDeleted());
                    submissionFinal.setFinishedEntryTime(v1Submission.getFinishedEntryTime());
                    submissionFinal.setStartEntryTime(v1Submission.getStartEntryTime());
                    submissionFinal.setActivity(v1Submission.getActivity());
                    submissionFinal.setForm(v1Submission.getForm());
                    submissionFinal.setTeamOld(v1Submission.getTeamOld());
                    submissionFinal.setTeam(v1Submission.getTeam());
                    submissionFinal.setAssignment(v1Submission.getAssignment());
                    submissionFinal.setStatus(v1Submission.getStatus());
                    submissionFinal.setVersion(v1Submission.getVersion());
                    submissionFinal.setFormData(v1Submission.getFormData());
                    submissionFinal.setCreatedBy(v1Submission.getCreatedBy());
                    submissionFinal.setLastModifiedBy(v1Submission.getLastModifiedBy());
                    mongoTemplate.save(submissionFinal);

                    mongoTemplate.remove(errorSubmissions.stream().filter((s) -> Objects.equals(s.getUid(),
                        v1Submission.getUid())).findFirst(), "data_form_submission_errors");

                } catch (Exception e) {
                    log.error("Failed to process submission: {}", v1Submission.getId(), e);
                    storeInDLQ(v1Submission, e);
                }
            });
        }
    }

    private void runMainStream() {
        Query formTemplateQuery = Query.query(Criteria.where("uid").is("Tcf3Ks9ZRpB"));

        DataForm formTemplate = mongoTemplate.findOne(formTemplateQuery, DataForm.class);

        Query query = Query.query(Criteria.where("form").is("Tcf3Ks9ZRpB"));

        AtomicInteger counter = new AtomicInteger();
        List<DataFormSubmissionFinal> batch = new ArrayList<>(BATCH_SIZE);

        try (Stream<DataFormSubmissionBu> stream =
                 mongoTemplate.stream(query, DataFormSubmissionBu.class)) {

            stream.forEach(v1Submission -> {
                try {
                    processAssignmentAndTeamSubmission(v1Submission);
                    migrate(v1Submission, formTemplate);
                    DataFormSubmissionFinal submissionFinal = new DataFormSubmissionFinal();

                    submissionFinal.setId(v1Submission.getId());
                    submissionFinal.setUid(v1Submission.getUid());
                    submissionFinal.setSerialNumber(v1Submission.getSerialNumber());
                    submissionFinal.setDeleted(v1Submission.getDeleted());
                    submissionFinal.setFinishedEntryTime(v1Submission.getFinishedEntryTime());
                    submissionFinal.setStartEntryTime(v1Submission.getStartEntryTime());
                    submissionFinal.setActivity(v1Submission.getActivity());
                    submissionFinal.setForm(v1Submission.getForm());
                    submissionFinal.setTeamOld(v1Submission.getTeamOld());
                    submissionFinal.setTeam(v1Submission.getTeam());
                    submissionFinal.setAssignment(v1Submission.getAssignment());
                    submissionFinal.setStatus(v1Submission.getStatus());
                    submissionFinal.setVersion(v1Submission.getVersion());
                    submissionFinal.setFormData(v1Submission.getFormData());
                    submissionFinal.setCreatedBy(v1Submission.getCreatedBy());
                    submissionFinal.setLastModifiedBy(v1Submission.getLastModifiedBy());

                    batch.add(submissionFinal);

                    if (batch.size() >= BATCH_SIZE) {
                        saveBatch(batch);
                        batch.clear();
                    }

                    if (counter.incrementAndGet() % LOG_INTERVAL == 0) {
                        log.info("Processed {} submissions", counter.get());
                    }
                } catch (Exception e) {
                    log.error("Failed to process submission: {}", v1Submission.getId(), e);
                    storeInDLQ(v1Submission, e);
                }
            });
        }
        // **Ensure remaining batch is saved after the loop**
        if (!batch.isEmpty()) {
            saveBatch(batch);
            batch.clear();
        }
    }

    private DataFormSubmissionBu processAssignmentAndTeamSubmission(DataFormSubmissionBu v1Submission) {

        try {
            Optional<String> orgUnit = Optional.ofNullable(JsonPath.read(v1Submission.getFormData(), "$.mainSection.locationStatus.orgUnit"));
            Optional<String> Status = Optional.ofNullable(resolvePath(v1Submission.getFormData(), "mainSection.locationStatus.status"));
            Optional<String> username = Optional.ofNullable(resolvePath(v1Submission.getFormData(), "_username"));
            Optional<String> activity = Optional.ofNullable(resolvePath(v1Submission.getFormData(), "_activity"));
            Optional<String> form = Optional.ofNullable(resolvePath(v1Submission.getFormData(), "_form"));
            Optional<String> id = Optional.ofNullable(resolvePath(v1Submission.getFormData(), "_id"));
            Optional<String> deviceId = Optional.ofNullable(resolvePath(v1Submission.getFormData(), "_deviceId"));

            v1Submission.getFormData().putIfAbsent("_serialNumber", v1Submission.getSerialNumber());
            v1Submission.getFormData().putIfAbsent("_deleted", v1Submission.getDeleted());

            deviceId.ifPresent((di) -> {
                v1Submission.getFormData().putIfAbsent("_deviceId", di);
            });

            id.ifPresent((i) -> {
                v1Submission.getFormData().putIfAbsent("_id", i);
            });

            form.ifPresent((f) -> {
                v1Submission.getFormData().putIfAbsent("_form", f);
            });

            Status.map(AssignmentStatus::valueOf).ifPresent((s) -> {
                v1Submission.setStatus(s);
                v1Submission.getFormData().putIfAbsent("_status", s.name());
            });
            activity.ifPresent((act) -> {
                v1Submission.setActivity(act);
                v1Submission.getFormData().putIfAbsent("_activity", act);
            });

            final var team = username.flatMap(s -> {
                v1Submission.getFormData().putIfAbsent("_username", s);
                return teamRepository.findFirstByActivityAndUser(v1Submission.getActivity(), s);
            }).orElse(null);

            if (team != null) {
                v1Submission.setTeam(team.getUid());
                v1Submission.getFormData().putIfAbsent("_team", team.getUid());
                final Optional<OrgUnit> orgU = orgUnit.flatMap(orgUnitRepository::findByUid);
                final Optional<Assignment> ass = orgU.flatMap((ou) -> assignmentRepository
                    .findFirstByTeamAndOrgUnit(team.getUid(), ou.getUid(), v1Submission.getActivity()));
                ass.ifPresent((assignment) -> {
                    v1Submission.setAssignment(assignment.getUid());
                    v1Submission.getFormData().putIfAbsent("_assignment", assignment.getUid());
                });
            }

            return v1Submission;
        } catch (Exception e) {
            throw new RuntimeException("Error processing team and assignment mapping", e);
        }
    }

    private DataFormSubmissionBu migrate(DataFormSubmissionBu v1Submission, DataForm v2Template) {
        Objects.requireNonNull(v1Submission, "v1Submission cannot be null");
        Objects.requireNonNull(v2Template, "v2Template cannot be null");

        v1Submission.setVersion(v2Template.getVersion());

        Map<String, ?> metaMap = Map.ofEntries(
            entry("_id", v1Submission.getFormData().get("_id")),
            entry("_deleted", v1Submission.getFormData().get("_deleted")),
            entry("_status", v1Submission.getFormData().get("_status")),
            entry("_assignment", v1Submission.getFormData().get("_assignment")),
            entry("_activity", v1Submission.getFormData().get("_activity")),
            entry("_team", v1Submission.getFormData().get("_team")),
            entry("_teamOld", v1Submission.getFormData().get("_teamOld")),
            entry("_serialNumber", v1Submission.getFormData().get("_serialNumber")),
            entry("_deviceId", v1Submission.getFormData().get("_deviceId"))
        );

        Map<String, Object> newFormData = new HashMap<>();

        migrateSection(v2Template.getFields(), v1Submission.getFormData(), newFormData, v1Submission.getUid());
        newFormData.putAll(metaMap);
        v1Submission.setFormData(newFormData);
        return v1Submission;
    }

    private void migrateSection(List<AbstractField> v2Fields, Map<String, Object> v1Data, Map<String, Object> v2Data, String submissionUid) {
        for (AbstractField field : v2Fields) {
            if (field instanceof Repeat repeatSection) {
                List<Map<String, Object>> repeats = processRepeats(repeatSection, v1Data, submissionUid);
                v2Data.put(field.getName(), repeats);
            } else if (field instanceof Section section) {
                Map<String, Object> nestedSection = new HashMap<>();
                migrateSection(section.getFields(), v1Data, nestedSection, submissionUid);
                v2Data.put(field.getName(), nestedSection);
            } else if (field instanceof DefaultField defaultField) {
                Object value = getMappedValue(defaultField, v1Data);
                v2Data.put(field.getName(), value != null ? value : defaultValuesMap.get(defaultField.getPath()));
            }
        }
    }

    private List<Map<String, Object>> processRepeats(Repeat repeatField, Map<String, Object> v1Data, String submissionUid) {
        String v1RepeatPath = fieldMappings.getOrDefault(repeatField.getPath(),
            anotherPathMap.getOrDefault(repeatField.getPath(), repeatField.getPath()));

        Object resolvedData = resolvePath(v1Data, v1RepeatPath);

        if (!(resolvedData instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> v1Repeats = (List<Map<String, Object>>) resolvedData;

        List<Map<String, Object>> repeats = new ArrayList<>();
        for (Map<String, Object> v1Repeat : v1Repeats) {
            Map<String, Object> v2Repeat = new HashMap<>();
            migrateSection(repeatField.getFields(), v1Repeat, v2Repeat, submissionUid);

            Map<String, ?> metaMap = Map.ofEntries(
                entry("_submissionUid", submissionUid),
                entry("_id", v1Repeat.get("_id")),
                entry("_parentId", v1Repeat.get("_parentId")),
                entry("_index", v1Repeat.get("_index"))
            );
            v2Repeat.putAll(metaMap);
            repeats.add(v2Repeat);
        }
        return repeats;
    }

    private Object getMappedValue(DefaultField v2Field, Map<String, Object> v1Data) {
        String v1Path = fieldMappings.getOrDefault(v2Field.getPath(),
            anotherPathMap.getOrDefault(v2Field.getPath(), v2Field.getPath()));

        return resolvePath(v1Data, v1Path); // Traverse v1Data using path
    }

    // Utility to resolve nested paths like "distributionData.householdDataOld"
    private <T> T resolvePath(Map<String, Object> data, String path) {
        String jPath = "$." + path;
        try {
            return JsonPath.read(data, jPath);
        } catch (Exception e) {
            String vPath = anotherPathMap.get(path);
            if (vPath != null) {
                try {
                    String aPath = "$." + vPath;
                    return JsonPath.read(data, aPath);
                } catch (Exception e2) {
                    return null;
                }

            }
            return null;
        }
    }


//    private void saveBatch(List<DataFormSubmissionFinal> batch) {
//        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DataFormSubmissionFinal.class);
//        bulkOps.insert(batch);
//        BulkWriteResult result = bulkOps.execute();
//        log.info("Bulk insert completed: {}", result);
//    }

    private void saveBatch(List<DataFormSubmissionFinal> batch) {
        try {
            BulkOperations bulkOps = mongoTemplate.bulkOps(
                BulkOperations.BulkMode.UNORDERED,
                DataFormSubmissionFinal.class
            );

            batch.forEach(bulkOps::insert);

            BulkWriteResult result = bulkOps.execute();
            log.debug("Saved batch of {} docs. Inserted: {}",
                batch.size(), result.getInsertedCount());

        } catch (Exception e) {
            log.error("Batch save failed for {} documents. Error: {}",
                batch.size(), e.getMessage());
            // Optionally retry individual documents
            retryFailedDocuments(batch, e);
        }
    }

    private void retryFailedDocuments(List<DataFormSubmissionFinal> batch, Exception originalError) {
        log.info("Retrying failed documents individually");

        batch.forEach(doc -> {
            try {
                mongoTemplate.save(doc);
            } catch (Exception e) {
                log.error("Permanent failure for document {}: {}",
                    doc.getUid(), e.getMessage());
                // Store in dead letter queue
                storeInDLQ(doc, originalError);
            }
        });
    }

    private <T extends Identifiable<String>> void storeInDLQ(T doc, Exception error) {
        try {
            // Convert entity to Map
            Map<String, Object> errorDocument = objectMapper.convertValue(doc, Map.class);

            // Append error details
            errorDocument.put("errorMessage", error.getMessage());
            errorDocument.put("errorStackTrace", getStackTraceAsString(error));
            errorDocument.put("failedAt", System.currentTimeMillis()); // Store timestamp
            errorDocument.put("failedAgain", true); // Store timestamp

            // Store in the custom error collection
            mongoTemplate.save(errorDocument, "data_form_submission_errors");

            log.info("Stored failed document {} in DLQ", doc.getUid());
        } catch (Exception e) {
            log.error("Failed to store document {} in DLQ: {}", doc.getUid(), e.getMessage());
        }
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
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
            "households_information.householdnames.householdHeadSerialNumber": "householdHeadSerialNumber",
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
            "status_cancelled_details.notargetingReasons": "mainSection.locationStatus.notargetingReasons",
            "mainSection.status": "mainSection.locationStatus.status",
            "status_cancelled_details.otherReasonForNotargeting": "mainSection.locationStatus.otherReasonForNotargeting",
            "households_information.householdnames.householdHeadSerialNumber": "householdNumber",
            "status_reassigned_details.targetedByOtherTeamName": "status_reassigned_details.targetedByOtherTeamName",
            "status_reassigned_details.targetedByOtherTeamNum": "status_reassigned_details.targetedByOtherTeamNum",
            "status_merged_details.mergedWithOtherVillage": "status_merged_details.mergedWithOtherVillage",
            "status_cancelled_details.notargetingReasons": "status_cancelled_details.notargetingReasons",
            "status_cancelled_details.otherReasonForNotargeting": "status_cancelled_details.otherReasonForNotargeting"
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
