package org.nmcpye.datarun;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.BaseMapper;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.JpaIdentifiableOperationVm;
import org.nmcpye.datarun.common.exceptions.DatabaseFailures;
import org.nmcpye.datarun.common.exceptions.ErrorCodeException;
import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.etl.repository.TallCanonicalJdbcRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommand;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datatemplate.CanonicalElement;
import org.nmcpye.datarun.jpa.datatemplate.DataType;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;
import org.nmcpye.datarun.jpa.datatemplate.TemplateElement;
import org.nmcpye.datarun.jpa.datatemplate.repository.CanonicalElementRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.MetadataUpsertService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.submission.feed.publisher.DictionaryPublisher;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.utils.UuidUtils;
import org.nmcpye.datarun.web.rest.v1.PivotController;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.NewCodeSeamsTest.Kind.CLIENT_CONTRACT;
import static org.nmcpye.datarun.NewCodeSeamsTest.Kind.NEUTRAL;
import static org.nmcpye.datarun.NewCodeSeamsTest.Kind.NEW_CODE;
import static org.nmcpye.datarun.NewCodeSeamsTest.Kind.RETIRING;
import static org.nmcpye.datarun.NewCodeSeamsTest.Kind.SYSTEM_OF_RECORD;

/**
 * New code does not couple to old structures: the owner's rule of 2026-11-25, as engineering refined it. The map, with
 * its reasons, is {@code docs/agent-context/legacy-seams.md}; this test holds the same lists.
 *
 * <p>Read and extend it in order. <b>1. The classification table</b> gives every main class exactly one kind: the
 * most specific row that matches it. <b>2. The allowlist</b> names the seams new code may use. <b>3. The rules</b>
 * check new code against both. A change to the map is a row or a seam, each with its reason. Nothing is frozen.</p>
 * <ul>
 *   <li>(a) Retiring derived structures: no dependency from new code, and none from them to new code, so neither shares
 *       the other's transaction, schedule or failures.</li>
 *   <li>(b) Systems of record: new code reaches them only through the listed seams.</li>
 *   <li>(c) Client contracts: new code never depends on v1 resources, their DTOs, mappers or v1 error codes.</li>
 *   <li>SQL text in new code, which ArchUnit cannot see, is scanned as a tripwire, not proof: it names no retiring
 *       relation, and system-of-record tables only in the seam that owns the read.</li>
 * </ul>
 */
class NewCodeSeamsTest {

    private static final String ROOT = "org.nmcpye.datarun";

    private static final JavaClasses MAIN = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ROOT);

    // =================================================================================================================
    // 1. The classification table
    // =================================================================================================================

    enum Kind {
        NEW_CODE("new code"),
        RETIRING("retiring derived code"),
        SYSTEM_OF_RECORD("system-of-record code"),
        CLIENT_CONTRACT("client contract code"),
        NEUTRAL("neutral infrastructure");

        final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    /**
     * Every main class is of exactly one kind: that of the most specific row matching it. A class row covers the class,
     * its nested classes and its JPA metamodel class; a package row covers that package ({@code "jpa.datasubmission"})
     * or that package and its subpackages ({@code "etl.."}). A class no row covers fails the test.
     */
    static final List<Row> CLASSIFICATION = List.of(
        // New code
        row("submission..", NEW_CODE, "the submission normalizer and the feed"),
        row("web.v2..", NEW_CODE, "the v2 API"),
        row("jpa.datasubmission.revision..", NEW_CODE, "the revision journal"),
        row("jpa.datasubmission.conflict..", NEW_CODE, "conflict detection (feat/review-s2)"),

        // Retiring derived structures
        row("etl..", RETIRING, "tall, events, dimensions and pivots, and the ETL that writes them"),
        row("outbox..", RETIRING, "the outbox, which only the ETL consumes and whose payload copies form_data"),
        row("jpa.datatemplategenerator..", RETIRING, "the canonical_element generator"),
        row(CanonicalElement.class, RETIRING, "canonical_element, which only the ETL and the pivots read"),
        row(CanonicalElementRepository.class, RETIRING, "canonical_element's repository"),
        row(MetadataUpsertService.class, RETIRING, "canonical_element's writer"),
        row(TemplateElement.class, RETIRING, "the generator's in-memory input"),
        row(SemanticType.class, RETIRING, "a canonical_element type"),
        row(DataType.class, RETIRING, "a canonical_element type"),
        row(PivotController.class, RETIRING, "the pivot route"),

        // Systems of record, with their old internals
        row("jpa.datasubmission", SYSTEM_OF_RECORD, "the submission entity"),
        row("jpa.datasubmission.command..", SYSTEM_OF_RECORD, "the single write path"),
        row("jpa.datasubmission.repository..", SYSTEM_OF_RECORD, "the submission repository"),
        row("jpa.datasubmission.service..", SYSTEM_OF_RECORD, "the submission persistence port"),
        row("jpa.datasubmission.validation..", SYSTEM_OF_RECORD, "the stored-record guard"),
        row("jpa.datasubmissionbatching..", SYSTEM_OF_RECORD, "repeat ids minted for old phones in the write path"),
        row("jpa.intake..", SYSTEM_OF_RECORD, "held uploads and go-live rules"),
        row("jpa.reference..", SYSTEM_OF_RECORD, "Reference lists"),
        row("jpa.datatemplate..", SYSTEM_OF_RECORD, "form templates and versions"),
        row("datatemplateelement..", SYSTEM_OF_RECORD, "form element configuration and its vocabulary"),
        row("datatemplateprocessor..", SYSTEM_OF_RECORD, "template processing, validation and Reference gates"),
        row("jpa.dataelement..", SYSTEM_OF_RECORD, "data elements"),
        row("jpa.option..", SYSTEM_OF_RECORD, "option sets"),
        row("jpa.assignment..", SYSTEM_OF_RECORD, "assignments"),
        row("jpa.team..", SYSTEM_OF_RECORD, "teams and their form grants"),
        row("jpa.orgunit..", SYSTEM_OF_RECORD, "org units"),
        row("jpa.orgunitgroup..", SYSTEM_OF_RECORD, "org unit groups"),
        row("jpa.orgunitgroupset..", SYSTEM_OF_RECORD, "org unit group sets"),
        row("jpa.oulevel..", SYSTEM_OF_RECORD, "org unit levels"),
        row("jpa.activity..", SYSTEM_OF_RECORD, "activities"),
        row("jpa.project..", SYSTEM_OF_RECORD, "projects"),
        row("jpa.user..", SYSTEM_OF_RECORD, "accounts"),
        row("jpa.userauthority..", SYSTEM_OF_RECORD, "authorities"),
        row("jpa.usegroup..", SYSTEM_OF_RECORD, "user groups"),
        row("jpa.userrefreshtoken..", SYSTEM_OF_RECORD, "refresh tokens"),
        row("jpa.accessfilter..", SYSTEM_OF_RECORD, "access scope: entity filters and form access"),
        row("jpa.migration..", SYSTEM_OF_RECORD, "the team form grant migration"),
        row("security..", SYSTEM_OF_RECORD, "the principal and the access snapshot built for each request"),
        row("userdetail..", SYSTEM_OF_RECORD, "form access in the access snapshot"),
        row("service..", SYSTEM_OF_RECORD, "account mail"),
        row("jpa.common..", SYSTEM_OF_RECORD, "generic entity bases, services and repositories"),
        row("jpa.auditing..", SYSTEM_OF_RECORD, "entity audit events"),
        row("common", SYSTEM_OF_RECORD, "generic identifiable objects and their services"),
        row("common.enumeration..", SYSTEM_OF_RECORD, "stored enumerations"),
        row("common.repository..", SYSTEM_OF_RECORD, "generic repository refusals"),
        row("common.translation..", SYSTEM_OF_RECORD, "label translations"),

        // Client contracts
        row("web.rest..", CLIENT_CONTRACT, "v1 routes, the inherited generic resources and v1 errors"),
        row("apiquery..", CLIENT_CONTRACT, "the v1 query language"),
        row("common.feedback..", CLIENT_CONTRACT, "v1 error codes, which the upload summary sends to phones"),
        row("jpa.assignment.dto..", CLIENT_CONTRACT, "v1 assignment payloads"),
        row("jpa.assignment.mapper..", CLIENT_CONTRACT, "v1 assignment mapping"),
        row("jpa.datatemplate.dto..", CLIENT_CONTRACT, "v1 form template payloads"),
        row("jpa.datatemplate.mapper..", CLIENT_CONTRACT, "v1 form template mapping"),
        row("jpa.user.dto..", CLIENT_CONTRACT, "v1 account payloads"),
        row("jpa.user.mapper..", CLIENT_CONTRACT, "v1 account mapping"),
        row("jpa.userrefreshtoken.dto..", CLIENT_CONTRACT, "v1 refresh payloads"),
        row("service.dto..", CLIENT_CONTRACT, "v1 account payloads"),
        row(EntitySaveSummaryVM.class, CLIENT_CONTRACT, "the upload summary phones read"),
        row(JpaIdentifiableOperationVm.class, CLIENT_CONTRACT, "a v1 generic write answer"),
        row(BaseMapper.class, CLIENT_CONTRACT, "the base of the v1 mappers"),
        row(BaseDto.class, CLIENT_CONTRACT, "the base of the v1 payloads"),
        row(ErrorCodeException.class, CLIENT_CONTRACT, "carries a v1 error code"),
        row(IllegalQueryException.class, CLIENT_CONTRACT, "a v1 query error"),
        row(FormSubmissionDataUtil.class, CLIENT_CONTRACT, "a helper of the v1 submission resource"),

        // Neutral infrastructure
        row(ROOT, NEUTRAL, "the application"),
        row("config..", NEUTRAL, "configuration, activation switches, security configuration"),
        row("aop..", NEUTRAL, "logging aspects"),
        row("management..", NEUTRAL, "security meters"),
        row("web.filter..", NEUTRAL, "request ids and request filters"),
        row(FailureShape.class, NEUTRAL, "failure shapes for log lines"),
        row(DatabaseFailures.class, NEUTRAL, "database failure classification"),
        row(CodeGenerator.class, NEUTRAL, "ids (nextUlid); its generateUid mints phone uids"),
        row(UuidUtils.class, NEUTRAL, "uuid helpers"));

    /** SQL relations, by kind: whole retiring schemas, retiring tables, and system-of-record tables (schema public). */
    static final Set<String> RETIRING_SCHEMAS = Set.of("analytics", "pivot");
    static final Set<String> RETIRING_TABLES = Set.of(
        "canonical_element", "template_element", "outbox", "outbox_processing", "etl_runs", "error_event");
    static final Set<String> SYSTEM_OF_RECORD_TABLES = Set.of(
        "data_submission", "held_submission", "go_live_rule", "reference_entry",
        "data_template", "data_template_version", "data_element", "option_set", "option_value", "option_group",
        "option_group_set", "assignment", "team", "team_user", "team_managed_teams", "activity", "project", "org_unit",
        "ou_level", "org_unit_group", "org_unit_group_members", "org_unit_groupset", "org_unit_groupset_org_unit_group",
        "app_user", "app_user_authority", "app_authority", "user_group", "refresh_token", "app_entity_audit_event");

    // =================================================================================================================
    // 2. The allowlist
    // =================================================================================================================

    /** Any new code may use the seam. */
    private static final String ANY_NEW_CODE = null;

    /**
     * The only system-of-record classes new code may depend on, each exactly (a nested class is not a seam unless
     * listed), from where and why. The normalizer ({@code submission.normalized}) and the journal
     * ({@code jpa.datasubmission.revision}) are seams too, but new code themselves, so not listed.
     */
    static final List<Seam> SEAMS = List.of(
        new Seam(SubmissionCommandService.class.getName(), ANY_NEW_CODE,
            "the single write path; its command variants are listed one by one, and none is yet: each of today's "
                + "carries a phone payload, the entity, a v1 request or a uid"),
        new Seam(SubmissionActor.class.getName(), ANY_NEW_CODE,
            "who a write-path command acts as; SubmissionActor.current() builds it from the request"),
        new Seam(ValueType.class.getName(), ANY_NEW_CODE,
            "the stored value-type vocabulary, carried by the normalizer's field definitions and values"),
        new Seam(CurrentUserDetails.class.getName(), ROOT + ".web.v2.caller..",
            "ApiCaller adapts the request's principal to its user id and authorities, and nothing else"),
        new Seam(DomainUserDetailsService.class.getName(), ROOT + ".web.v2.config..",
            "the v2 chain resolves the bearer token's account as v1 does: authentication, not scope"));

    /** Where SQL in new code may name system-of-record tables: the seam that owns the read, by source path prefix. */
    static final List<SqlSeam> SQL_SEAMS = List.of(
        new SqlSeam("org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java",
            Set.of("data_template", "data_template_version", "data_element", "option_set"),
            "the normalizer's form definitions"),
        new SqlSeam("org/nmcpye/datarun/jpa/datasubmission/revision/", Set.of("data_submission"),
            "the journal copies the stored row it records"));

    // =================================================================================================================
    // 3. The rules, built from their predicates so that the fixtures below can show each failing
    // =================================================================================================================

    /** (a) New code takes no dependency on retiring code. */
    static ArchRule newCodeDependsOnNoRetiringCode(DescribedPredicate<JavaClass> newCode) {
        return noClasses().that(newCode).should().dependOnClassesThat(ofKind(RETIRING))
            .because("retiring derived structures get no dependency: no read, no write, no import (rule a)");
    }

    /** (a) Retiring code takes no dependency on new code. */
    static ArchRule retiringCodeDependsOnNoNewCode(DescribedPredicate<JavaClass> retiring,
                                                   DescribedPredicate<JavaClass> newCode) {
        return noClasses().that(retiring).should().dependOnClassesThat(newCode)
            .because("retiring code that calls new code shares its transaction, its schedule and its failures with it "
                + "(rule a)");
    }

    /** (b) New code reaches systems of record only through the seams. */
    static ArchRule newCodeReachesSystemsOfRecordOnlyThroughSeams(DescribedPredicate<JavaClass> newCode) {
        return classes().that(newCode).should(dependOnlyThroughSeams(Set.of(SYSTEM_OF_RECORD)))
            .because("systems of record are reached through one named seam each, never through their entities, "
                + "repositories or generic services (rule b)");
    }

    /** (c) New code takes no dependency on client contracts. */
    static ArchRule newCodeDependsOnNoClientContract(DescribedPredicate<JavaClass> newCode) {
        return noClasses().that(newCode).should().dependOnClassesThat(ofKind(CLIENT_CONTRACT))
            .because("installed clients' contracts are adapted at the edge; their shapes, uids and error codes never "
                + "travel inward (rule c)");
    }

    /** SQL in new code: one finding per retiring relation, and per system-of-record table outside its seam. */
    static List<String> sqlFindings(String path, String source) {
        List<String> findings = new ArrayList<>();
        for (String literal : SubmissionWriteOwnershipTest.stringLiterals(source)) {
            Matcher relation = RELATION.matcher(literal);
            while (relation.find()) {
                String schema = relation.group(1) == null ? null : relation.group(1).toLowerCase(Locale.ROOT);
                String table = relation.group(2).toLowerCase(Locale.ROOT);
                boolean inPublic = schema == null || schema.equals("public");
                if (schema != null && RETIRING_SCHEMAS.contains(schema)) {
                    findings.add(path + ": retiring relation " + schema + "." + table);
                } else if (inPublic && RETIRING_TABLES.contains(table)) {
                    findings.add(path + ": retiring relation " + table);
                } else if (inPublic && SYSTEM_OF_RECORD_TABLES.contains(table) && SQL_SEAMS.stream()
                    .noneMatch(seam -> path.startsWith(seam.source()) && seam.tables().contains(table))) {
                    findings.add(path + ": system-of-record table " + table + " outside its seam");
                }
            }
        }
        return findings.stream().distinct().toList();
    }

    // The tests: the rules on the main classes.

    @Test
    void newCodeTakesNoDependencyOnRetiringCode() {
        newCodeDependsOnNoRetiringCode(ofKind(NEW_CODE)).check(MAIN);
    }

    @Test
    void retiringCodeTakesNoDependencyOnNewCode() {
        retiringCodeDependsOnNoNewCode(ofKind(RETIRING), ofKind(NEW_CODE)).check(MAIN);
    }

    @Test
    void newCodeReachesSystemsOfRecordOnlyThroughTheirSeams() {
        newCodeReachesSystemsOfRecordOnlyThroughSeams(ofKind(NEW_CODE)).check(MAIN);
    }

    @Test
    void newCodeTakesNoDependencyOnClientContracts() {
        newCodeDependsOnNoClientContract(ofKind(NEW_CODE)).check(MAIN);
    }

    @Test
    void sqlInNewCodeNamesNoRetiringRelationAndSystemsOfRecordOnlyInTheirSeams() throws IOException {
        List<String> findings = new ArrayList<>();
        int scanned = 0;
        Path sources = Path.of("src", "main", "java");
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String path = sources.relativize(file).toString().replace('\\', '/');
                if (ofKind(NEW_CODE).test(sourceClass(path))) {
                    scanned++;
                    findings.addAll(sqlFindings(path, Files.readString(file)));
                }
            }
        }
        assertThat(scanned).as("new code's sources found").isPositive();
        assertThat(findings).as("SQL text in new code naming a retiring relation, or a system-of-record table "
            + "outside its seam (a tripwire, not proof: SQL built at run time is not seen)").isEmpty();
    }

    // The tests: the table and the allowlist.

    /** Unclassified code would escape every rule. */
    @Test
    void everyMainClassHasAKind() {
        List<String> unclassified = MAIN.stream()
            .filter(c -> !c.getSimpleName().equals("package-info"))
            .filter(c -> rowOf(c).isEmpty())
            .map(JavaClass::getName).sorted().toList();
        assertThat(MAIN.size()).as("classes imported").isGreaterThan(300);
        assertThat(unclassified).as("main classes no row covers (add a row to CLASSIFICATION)").isEmpty();
    }

    /**
     * Each row names its target once, with a reason. A row may name a package that only a branch holds so far (conflict
     * detection), so a row that classifies nothing on this branch is not an error.
     */
    @Test
    void everyRowIsUniqueAndHasAReason() {
        assertThat(CLASSIFICATION.stream().map(Row::target).distinct().count()).as("rows naming the same target")
            .isEqualTo(CLASSIFICATION.size());
        assertThat(CLASSIFICATION).allSatisfy(r -> assertThat(r.why()).as(r.target()).isNotBlank());
    }

    /** A seam is old code that stays, with a reason. */
    @Test
    void everySeamIsSystemOfRecordCode() {
        for (Seam seam : SEAMS) {
            assertThat(kindOf(MAIN.get(seam.target()))).as(seam.target()).contains(SYSTEM_OF_RECORD);
            assertThat(seam.why()).as(seam.target()).isNotBlank();
        }
    }

    // The tests: the rules can fail.

    /** Each kind of leak is caught, and what the seams allow is not. */
    @Test
    void theRulesCatchALeak() {
        JavaClasses classes = new ClassFileImporter().importClasses(Leak.class, EtlLeak.class);
        DescribedPredicate<JavaClass> leak = belongToAnyOf(Leak.class);

        assertThat(details(newCodeDependsOnNoRetiringCode(leak), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("TallCanonicalJdbcRepository"));
        assertThat(details(retiringCodeDependsOnNoNewCode(belongToAnyOf(EtlLeak.class), ofKind(NEW_CODE)), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("DictionaryPublisher"));
        assertThat(details(newCodeReachesSystemsOfRecordOnlyThroughSeams(leak), classes))
            .anySatisfy(line -> assertThat(line).contains("DataSubmission>"))
            .anySatisfy(line -> assertThat(line).contains("SubmissionCommand$Delete"))
            .anySatisfy(line -> assertThat(line).contains("CurrentUserDetails"))
            .noneSatisfy(line -> assertThat(line).containsAnyOf("SubmissionCommandService>", "ValueType"));
        assertThat(details(newCodeDependsOnNoClientContract(leak), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("DataSubmissionUploadV1Dto"));
    }

    /** The scan's rules on small sources. */
    @Test
    void theSqlScanFlagsWhatItShould() {
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java",
            "String head = \"SELECT s.id FROM data_submission s LEFT JOIN public.team tm ON tm.uid = s.team_uid\";"))
            .containsExactly(
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table data_submission outside its seam",
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table team outside its seam");
        assertThat(sqlFindings("org/nmcpye/datarun/web/v2/X.java", String.join("\n",
            "String tall = \"\"\"", "    INSERT INTO analytics.tall_canonical VALUES (1)\"\"\";",
            "String old = \"UPDATE \\\"canonical_element\\\" SET name = 'x'\";")))
            .containsExactly(
                "org/nmcpye/datarun/web/v2/X.java: retiring relation analytics.tall_canonical",
                "org/nmcpye/datarun/web/v2/X.java: retiring relation canonical_element");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java",
            "String v = \"FROM data_template t JOIN data_template_version v ON v.data_template_id = t.id\";"))
            .as("the normalizer's own reads").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/jpa/datasubmission/revision/Reader.java",
            "String v = \"SELECT form_data FROM data_submission JOIN journal.submission_revision r ON true\";"))
            .as("the journal's own reads").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/submission/normalized/jdbc/Other.java",
            "String v = \"SELECT 1 FROM data_template\";"))
            .as("a seam's tables in another class of its package")
            .containsExactly("org/nmcpye/datarun/submission/normalized/jdbc/Other.java: system-of-record table "
                + "data_template outside its seam");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java",
            "// SELECT * FROM data_submission in a comment\nString w = \"INSERT INTO feed.submission VALUES (1)\";"))
            .as("comments, and the new schemas").isEmpty();
    }

    // Fixtures for the tests above.

    /** Stands for new code: one leak of each kind, and one use of each seam it may use. */
    @SuppressWarnings("unused")
    static final class Leak {
        void leaks(TallCanonicalJdbcRepository tall, DataSubmission entity, CurrentUserDetails principal,
                   DataSubmissionUploadV1Dto payload) {
            new SubmissionCommand.Delete(null, "a uid");
        }

        void allowed(SubmissionCommandService commands, ValueType type, FailureShape shape) {
        }
    }

    /** Stands for retiring code that calls into new code. */
    @SuppressWarnings("unused")
    static final class EtlLeak {
        boolean publish(DictionaryPublisher feed) {
            return feed.publishIfChanged();
        }
    }

    // The table's and the allowlist's mechanics.

    /** A row of the classification: a package pattern or a class name, its kind, and why. */
    record Row(String target, boolean isClass, Kind kind, String why) {

        /** For a package row: how specific it is, or -1 when it does not cover the package. */
        int specificity(String packageName) {
            boolean withSubpackages = target.endsWith("..");
            String base = withSubpackages ? target.substring(0, target.length() - 2) : target;
            boolean covers = packageName.equals(base) || (withSubpackages && packageName.startsWith(base + "."));
            return covers ? 2 * base.length() + (withSubpackages ? 0 : 1) : -1;
        }
    }

    private static Row row(String relativePackage, Kind kind, String why) {
        return new Row(relativePackage.equals(ROOT) ? ROOT : ROOT + "." + relativePackage, false, kind, why);
    }

    private static Row row(Class<?> type, Kind kind, String why) {
        return new Row(type.getName(), true, kind, why);
    }

    /** A seam: exactly this class, usable from new code in these packages ({@code null}: any new code), and why. */
    record Seam(String target, String usedFrom, String why) {

        boolean allows(JavaClass origin, JavaClass target) {
            return target.getName().equals(this.target)
                && (usedFrom == null || PackageMatcher.of(usedFrom).matches(origin.getPackageName()));
        }
    }

    /** A seam's own SQL: the source (a file, or a package folder) where new code may name these tables, and why. */
    record SqlSeam(String source, Set<String> tables, String why) {
    }

    private static final Map<String, Optional<Row>> ROWS_BY_CLASS = new ConcurrentHashMap<>();

    /** The most specific row covering the class: a class row first, then the most specific package row. */
    static Optional<Row> rowOf(JavaClass javaClass) {
        return ROWS_BY_CLASS.computeIfAbsent(javaClass.getName(), name -> {
            String top = name.split("\\$", 2)[0];
            String entity = top.endsWith("_") ? top.substring(0, top.length() - 1) : top;
            Optional<Row> byClass = CLASSIFICATION.stream()
                .filter(r -> r.isClass() && (r.target().equals(top) || r.target().equals(entity))).findFirst();
            if (byClass.isPresent()) {
                return byClass;
            }
            String packageName = javaClass.getPackageName();
            return CLASSIFICATION.stream().filter(r -> !r.isClass() && r.specificity(packageName) >= 0)
                .max(Comparator.comparingInt(r -> r.specificity(packageName)));
        });
    }

    static Optional<Kind> kindOf(JavaClass javaClass) {
        return rowOf(javaClass).map(Row::kind);
    }

    static DescribedPredicate<JavaClass> ofKind(Kind kind) {
        return DescribedPredicate.describe(kind.label, c -> kindOf(c.getBaseComponentType()).orElse(null) == kind);
    }

    /** The class a main source file declares, for the SQL scan to classify its file like its class. */
    private static JavaClass sourceClass(String path) {
        String name = path.substring(0, path.length() - ".java".length()).replace('/', '.');
        return MAIN.contain(name) ? MAIN.get(name) : MAIN.get(DataRunApiApp.class);
    }

    /** Each dependency on code of these kinds that no seam allows is a violation, reported with its location. */
    private static ArchCondition<JavaClass> dependOnlyThroughSeams(Set<Kind> guarded) {
        return new ArchCondition<>("depend on " + guarded.stream().map(k -> k.label).collect(Collectors.joining(" or "))
            + " only through the seams") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    if (kindOf(target).filter(guarded::contains).isPresent()
                        && SEAMS.stream().noneMatch(seam -> seam.allows(origin, target))) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static List<String> details(ArchRule rule, JavaClasses classes) {
        return rule.evaluate(classes).getFailureReport().getDetails();
    }

    /** An optional double quote, escaped or not: Java source text holds quoted identifiers as \". */
    private static final String QUOTE = "(?:\\\\?\")?";
    /** A relation after an SQL word that takes one, schema-qualified or not. */
    private static final Pattern RELATION = Pattern.compile("(?i)\\b(?:from|join|into|update|truncate|table|copy)\\s+"
        + "(?:only\\s+)?(?:" + QUOTE + "(\\w+)" + QUOTE + "\\s*\\.\\s*)?" + QUOTE + "(\\w+)");
}
