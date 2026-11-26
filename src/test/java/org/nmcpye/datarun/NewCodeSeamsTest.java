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
import org.nmcpye.datarun.common.LatinDigits;
import org.nmcpye.datarun.common.exceptions.DatabaseFailures;
import org.nmcpye.datarun.common.exceptions.ErrorCodeException;
import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.pivot.PivotService;
import org.nmcpye.datarun.etl.repository.TallCanonicalJdbcRepository;
import org.nmcpye.datarun.etl.service.EtlRunService;
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
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceServiceImpl;
import org.nmcpye.datarun.jpa.datatemplategenerator.TemplateElementGeneratorService;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.service.impl.OutboxClaimServiceImpl;
import org.nmcpye.datarun.outbox.service.impl.OutboxProcessingServiceImpl;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.submission.feed.publisher.DictionaryPublisher;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.utils.UuidUtils;
import org.nmcpye.datarun.web.filter.LastSeenFilter;
import org.nmcpye.datarun.web.rest.v1.PivotController;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import static org.nmcpye.datarun.NewCodeSeamsTest.Kind.TRANSITIONAL;

/**
 * New code does not couple to old structures: the owner's rule of 2026-11-25, as engineering refined it. The map, with
 * its reasons, is {@code docs/agent-context/legacy-seams.md}; this test holds the same lists.
 *
 * <p>Read and extend it in order. <b>1. The classification table</b> gives every main class exactly one kind: the
 * most specific row that matches it. <b>2. The allowlist</b> names the seams new code may use. <b>3. The rules</b>
 * check new code against both. A change to the map is a row, a seam or an exception, each with its reason. Nothing is
 * frozen: an exception names its class and dependency, and the test fails once it no longer occurs.</p>
 * <ul>
 *   <li>(a) Retiring derived structures: no dependency from new code, and none from them to new code, so a legacy
 *       output's failure never aborts new work, and delays it by at most one retry. Code that stays depends on them
 *       only where {@link #LEFTOVERS} lists the class and the dependency.</li>
 *   <li>(b) Systems of record and transitional code: new code reaches them only through the listed seams.</li>
 *   <li>(c) Client contracts: new code never depends on v1 resources, their DTOs, mappers or v1 error codes.</li>
 *   <li>SQL text in new code, which ArchUnit cannot see, is scanned as a tripwire, not proof: it names no retiring
 *       or transitional relation, and system-of-record tables only in the seam that owns the read.</li>
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
        TRANSITIONAL("transitional code"),
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
        row("jpa.datasubmission.conflict..", NEW_CODE, "conflict detection, called by the write path, which gives it "
            + "plain values"),

        // Retiring derived structures
        row("etl..", RETIRING, "tall, events, dimensions and pivots, their transformers, and for now the home and name "
            + "of the outbox's claim loop"),
        row("jpa.datatemplategenerator..", RETIRING, "the canonical_element generator"),
        row(CanonicalElement.class, RETIRING, "canonical_element, which only the ETL and the pivots read"),
        row(CanonicalElementRepository.class, RETIRING, "canonical_element's repository"),
        row(MetadataUpsertService.class, RETIRING, "canonical_element's writer"),
        row(TemplateElement.class, RETIRING, "the generator's in-memory input"),
        row(SemanticType.class, RETIRING, "a canonical_element type"),
        row(DataType.class, RETIRING, "a canonical_element type"),
        row(PivotController.class, RETIRING, "the pivot route"),

        // Transitional: stays for now; whether the journal replaces it is an open design question
        row("outbox..", TRANSITIONAL, "the consumers' change queue and the gap repair's cue; its payload copy retires"),

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
        row(LastSeenFilter.class, CLIENT_CONTRACT, "an edge adapter: reads the phones' X-App-Version header and "
            + "records the account's last visit"),

        // Neutral infrastructure
        row(ROOT, NEUTRAL, "the application"),
        row("config..", NEUTRAL, "configuration, activation switches, security configuration"),
        row("aop..", NEUTRAL, "logging aspects"),
        row("management..", NEUTRAL, "security meters"),
        row("web.filter..", NEUTRAL, "request ids and request filters"),
        row(FailureShape.class, NEUTRAL, "failure shapes for log lines"),
        row(DatabaseFailures.class, NEUTRAL, "database failure classification"),
        row(CodeGenerator.class, NEUTRAL, "ids (nextUlid); its generateUid mints phone uids"),
        row(UuidUtils.class, NEUTRAL, "uuid helpers"),
        row(LatinDigits.class, NEUTRAL, "a pure digit utility"));

    /**
     * SQL relations, by kind: whole retiring schemas, retiring tables, transitional tables (which new code reaches only
     * through {@code outbox.processing}) and system-of-record tables, all in schema public unless named.
     */
    static final Set<String> RETIRING_SCHEMAS = Set.of("analytics", "pivot");
    static final Set<String> RETIRING_TABLES = Set.of(
        "canonical_element", "canonical_element_anchor", "template_element", "etl_runs", "error_event", "outbox_event");
    static final Set<String> TRANSITIONAL_TABLES = Set.of("outbox", "outbox_processing");
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
    /** An SQL seam that exists, or one planned and not built yet. */
    private static final boolean BUILT = false;
    private static final boolean PLANNED = true;

    /**
     * The only system-of-record and transitional code new code may depend on: a class exactly (a nested class is not a
     * seam unless listed), or a package with its subpackages ({@code "x.."}); from where, and why. The normalizer
     * ({@code submission.normalized}) and the journal ({@code jpa.datasubmission.revision}) are seams too, but new code
     * themselves, so not listed.
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
            "the v2 chain resolves the bearer token's account as v1 does: authentication, not scope"),
        new Seam(ROOT + ".outbox.processing..", ANY_NEW_CODE,
            "the outputs' seam: ChangeOutput and ChangeReference (a change as a submission id and version, wherever "
                + "changes come from), DeferredWork (an output's work behind the live changes) and ChangeBacklog (the "
                + "changes recorded and not yet processed); never the outbox's table, rows or services"));

    /**
     * Code that stays and still depends on retiring code: each by its class and the class it depends on (nested
     * classes included on both sides), with why. Only that dependency is exempt, and the test fails once it no longer
     * occurs.
     */
    static final List<Leftover> LEFTOVERS = List.of(
        new Leftover(OutboxClaimServiceImpl.class, EtlProperties.class,
            "reads its claim lease from the ETL's properties, until the claim loop leaves etl"),
        new Leftover(OutboxProcessingServiceImpl.class, EtlRunService.class,
            "counts outcomes on the ETL's run, until the claim loop leaves etl"),
        new Leftover(DataTemplateInstanceServiceImpl.class, TemplateElementGeneratorService.class,
            "publishing a form version writes canonical_element in the same call, until canonical_element goes"));

    /**
     * Breaches by new code that are listed until a named change removes them, each by its class (nested classes
     * included) and the class it depends on. A listed breach must still occur, so the list only shrinks; none is
     * listed now.
     */
    static final List<Breach> BREACHES = List.of();

    /**
     * Where SQL in new code may name system-of-record tables: the seam that owns the read, by source path prefix.
     * {@code data_submission} and the configuration tables are named only in these readers and the journal (and,
     * outside new code, in the write path).
     */
    static final List<SqlSeam> SQL_SEAMS = List.of(
        new SqlSeam("org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java",
            Set.of("data_template", "data_template_version", "data_element", "option_set"), BUILT,
            "the normalizer's form definitions, and a form's and a version's ids by uid"),
        // Planned by engineering (2026-11-26) and built for the feed.
        new SqlSeam("org/nmcpye/datarun/submission/normalized/jdbc/StoredSubmissionJdbcReader.java",
            Set.of("data_submission"), BUILT,
            "stored submissions by id and in batches, as StoredSubmission"),
        new SqlSeam("org/nmcpye/datarun/submission/normalized/jdbc/PlacementJdbcResolver.java",
            Set.of("assignment", "team", "org_unit", "activity"), BUILT,
            "one placement resolver: a submission's assignment, team, org unit and activity uids turned into ids"),
        new SqlSeam("org/nmcpye/datarun/jpa/datasubmission/revision/", Set.of("data_submission"), BUILT,
            "the journal copies the stored row it records"));

    // =================================================================================================================
    // 3. The rules, built from their predicates so that the fixtures below can show each failing
    // =================================================================================================================

    /** (a) New code takes no dependency on retiring code. */
    static ArchRule newCodeDependsOnNoRetiringCode(DescribedPredicate<JavaClass> newCode) {
        return classes().that(newCode).should(dependOnGuardedCodeOnlyAsListed(Set.of(RETIRING), false))
            .because("retiring derived structures get no dependency: no read, no write, no import (rule a)");
    }

    /** (a) Retiring code takes no dependency on new code. */
    static ArchRule retiringCodeDependsOnNoNewCode(DescribedPredicate<JavaClass> retiring,
                                                   DescribedPredicate<JavaClass> newCode) {
        return noClasses().that(retiring).should().dependOnClassesThat(newCode)
            .because("a legacy output's failure never aborts new work, and delays it by at most one retry (rule a)");
    }

    /** (a) Code that stays takes no dependency on retiring code, beyond the listed leftovers. */
    static ArchRule codeThatStaysDependsOnRetiringCodeOnlyAsListed(DescribedPredicate<JavaClass> stays,
                                                                    List<Leftover> leftovers) {
        return classes().that(stays).should(dependOnRetiringCodeOnlyAs(leftovers))
            .because("what stays must not hold up what retires: retiring code is removed without touching it (rule a)");
    }

    /** (b) New code reaches systems of record and transitional code only through the seams. */
    static ArchRule newCodeReachesOldCodeOnlyThroughSeams(DescribedPredicate<JavaClass> newCode) {
        return classes().that(newCode)
            .should(dependOnGuardedCodeOnlyAsListed(Set.of(SYSTEM_OF_RECORD, TRANSITIONAL), true))
            .because("systems of record and the outbox are reached through one named seam each, never through their "
                + "entities, tables, repositories or generic services (rule b)");
    }

    /** (c) New code takes no dependency on client contracts. */
    static ArchRule newCodeDependsOnNoClientContract(DescribedPredicate<JavaClass> newCode) {
        return classes().that(newCode).should(dependOnGuardedCodeOnlyAsListed(Set.of(CLIENT_CONTRACT), false))
            .because("installed clients' contracts are adapted at the edge; their shapes, uids and error codes never "
                + "travel inward (rule c)");
    }

    /**
     * SQL in new code: one finding per retiring or transitional relation, and per system-of-record table outside its
     * seam. A tripwire, not proof. It reads only literals with the structure of a query, in any case: select ... from,
     * insert into, update [only] table [alias] set, delete from, merge into ... using, with ... as (. So most prose
     * naming a table is not flagged; prose shaped like a query is, a known false positive ("Delete from team {}").
     * Names compare as written: tables are lowercase here, and JPQL names entities (Activity), so JPQL is not
     * flagged. It does not see a table name written in uppercase, a fragment without that structure joined to others
     * at run time, names assembled from constants, a FROM list item after a nested parenthesis, or tables reached
     * through views, functions or a search path.
     */
    static List<String> sqlFindings(String path, String source) {
        List<String> findings = new ArrayList<>();
        for (String literal : JavaLiterals.strings(source)) {
            if (!SQL_LIKE.matcher(literal).find()) {
                continue;
            }
            for (String[] relation : relations(literal)) {
                String schema = relation[0];
                String table = relation[1];
                boolean inPublic = schema == null || schema.equals("public");
                if (schema != null && RETIRING_SCHEMAS.contains(schema)) {
                    findings.add(path + ": retiring relation " + schema + "." + table);
                } else if (inPublic && RETIRING_TABLES.contains(table)) {
                    findings.add(path + ": retiring relation " + table);
                } else if (inPublic && TRANSITIONAL_TABLES.contains(table)) {
                    findings.add(path + ": transitional table " + table + " (new code uses outbox.processing)");
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
    void codeThatStaysTakesNoDependencyOnRetiringCodeBeyondTheLeftovers() {
        codeThatStaysDependsOnRetiringCodeOnlyAsListed(STAYS, LEFTOVERS).check(MAIN);
    }

    @Test
    void newCodeReachesSystemsOfRecordAndTransitionalCodeOnlyThroughSeams() {
        newCodeReachesOldCodeOnlyThroughSeams(ofKind(NEW_CODE)).check(MAIN);
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
        assertThat(findings).as("SQL text in new code naming a retiring or transitional relation, or a "
            + "system-of-record table outside its seam (a tripwire, not proof: SQL built at run time is not seen)")
            .isEmpty();
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

    /** A built SQL seam exists where named; a planned one does not yet, and is marked built once it does. */
    @Test
    void everySqlSeamIsWhereItsMarkSays() {
        for (SqlSeam seam : SQL_SEAMS) {
            boolean exists = Files.exists(Path.of("src", "main", "java").resolve(seam.source()));
            assertThat(exists).as(seam.source() + (seam.planned() ? " exists: mark it BUILT" : " is missing"))
                .isEqualTo(!seam.planned());
            assertThat(seam.why()).as(seam.source()).isNotBlank();
        }
    }

    /** A seam is system-of-record or transitional code, with a reason; a leftover still needs its place. */
    @Test
    void everySeamAndLeftoverIsWhatItSays() {
        for (Seam seam : SEAMS) {
            Optional<Kind> kind = seam.isPackage() ? kindOfPackage(seam.packageName())
                : kindOf(MAIN.get(seam.target()));
            assertThat(kind.orElse(null)).as(seam.target()).isIn(SYSTEM_OF_RECORD, TRANSITIONAL);
            assertThat(seam.why()).as(seam.target()).isNotBlank();
        }
        for (Leftover leftover : LEFTOVERS) {
            String pair = leftover.origin().getName() + " on " + leftover.dependency().getName();
            assertThat(STAYS.test(MAIN.get(leftover.origin()))).as(pair + ": the class stays").isTrue();
            assertThat(kindOf(MAIN.get(leftover.dependency()))).as(pair + ": the dependency retires")
                .contains(RETIRING);
            assertThat(leftover.why()).as(pair).isNotBlank();
            assertThat(occurs(leftover.origin().getName(), leftover.dependency().getName()))
                .as(pair + " no longer occurs: remove the leftover").isTrue();
        }
    }

    /** A listed breach still occurs, in new code, on a dependency a rule guards. */
    @Test
    void everyBreachStillOccurs() {
        for (Breach breach : BREACHES) {
            assertThat(breach.why()).as(breach.origin()).isNotBlank();
            assertThat(kindOf(MAIN.get(breach.dependency())).orElse(null)).as(breach.dependency())
                .isIn(RETIRING, TRANSITIONAL, SYSTEM_OF_RECORD, CLIENT_CONTRACT);
            assertThat(MAIN.contain(breach.origin())).as(breach.origin() + " is not in this build: remove the breach")
                .isTrue();
            assertThat(kindOf(MAIN.get(breach.origin()))).as(breach.origin()).contains(NEW_CODE);
            assertThat(occurs(breach.origin(), breach.dependency()))
                .as(breach.origin() + " no longer depends on " + breach.dependency() + ": remove the breach")
                .isTrue();
        }
    }

    // The tests: the rules can fail.

    /** Each kind of leak is caught, and what the seams allow is not. */
    @Test
    void theRulesCatchALeak() {
        JavaClasses classes = new ClassFileImporter()
            .importClasses(Leak.class, EtlLeak.class, StayingLeak.class);
        DescribedPredicate<JavaClass> leak = belongToAnyOf(Leak.class);

        assertThat(details(newCodeDependsOnNoRetiringCode(leak), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("TallCanonicalJdbcRepository"));
        assertThat(details(retiringCodeDependsOnNoNewCode(belongToAnyOf(EtlLeak.class), ofKind(NEW_CODE)), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("DictionaryPublisher"));
        List<Leftover> listed = List.of(new Leftover(StayingLeak.class, EtlProperties.class, "listed, for this test"));
        assertThat(details(codeThatStaysDependsOnRetiringCodeOnlyAsListed(belongToAnyOf(StayingLeak.class), listed),
            classes))
            .as("a leftover exempts its listed dependency only")
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("PivotService").doesNotContain("EtlProperties"));
        assertThat(details(newCodeReachesOldCodeOnlyThroughSeams(leak), classes))
            .anySatisfy(line -> assertThat(line).contains("DataSubmission>"))
            .anySatisfy(line -> assertThat(line).contains("OutboxDto"))
            .anySatisfy(line -> assertThat(line).contains("SubmissionCommand$Delete"))
            .anySatisfy(line -> assertThat(line).contains("CurrentUserDetails"))
            .noneSatisfy(line -> assertThat(line).containsAnyOf("SubmissionCommandService>", "ValueType"));
        assertThat(details(newCodeDependsOnNoClientContract(leak), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("DataSubmissionUploadV1Dto"));
    }

    /**
     * The classification alone catches a back door: a class in the submission packages, or in the outbox, that reads
     * the ETL's settings breaks rule (a) by its package, with no predicate written for it.
     */
    @Test
    void theClassificationCatchesABackDoorByItsPackage() {
        JavaClasses classes = new ClassFileImporter().importClasses(org.nmcpye.datarun.submission.LegacyBackDoor.class,
            org.nmcpye.datarun.outbox.LegacyBackDoor.class, EtlProperties.class);

        assertThat(details(newCodeDependsOnNoRetiringCode(ofKind(NEW_CODE)), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("submission.LegacyBackDoor"));
        assertThat(details(codeThatStaysDependsOnRetiringCodeOnlyAsListed(STAYS, LEFTOVERS), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("outbox.LegacyBackDoor"));
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
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java",
            "String backlog = \"SELECT count(*) FROM outbox o WHERE o.status = 'pending'\";"))
            .containsExactly("org/nmcpye/datarun/submission/feed/X.java: transitional table outbox (new code uses "
                + "outbox.processing)");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java",
            "String v = \"FROM data_template t JOIN data_template_version v ON v.data_template_id = t.id\";"))
            .as("the normalizer's own reads").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/jpa/datasubmission/revision/Reader.java",
            "String v = \"SELECT form_data FROM data_submission JOIN journal.submission_revision r ON true\";"))
            .as("the journal's own reads").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/submission/normalized/jdbc/PlacementJdbcResolver.java",
            "String v = \"SELECT a.id FROM assignment a JOIN team t ON t.id = a.team_id\";"))
            .as("a seam's own reads").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/publisher/Placement.java",
            "String v = \"SELECT a.id FROM assignment a WHERE a.uid = :assignmentUid\";"))
            .as("the same reads anywhere else").containsExactly("org/nmcpye/datarun/submission/feed/publisher/"
                + "Placement.java: system-of-record table assignment outside its seam");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/normalized/jdbc/Other.java",
            "String v = \"SELECT 1 FROM data_template\";"))
            .as("a seam's tables in another class of its package")
            .containsExactly("org/nmcpye/datarun/submission/normalized/jdbc/Other.java: system-of-record table "
                + "data_template outside its seam");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java",
            "// SELECT * FROM data_submission in a comment\nString w = \"INSERT INTO feed.submission VALUES (1)\";"))
            .as("comments, and the new schemas").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java", String.join("\n",
            "log.info(\"Submission {} moved from assignment {} with cause {}\", id, from, cause);",
            "String hint = \"Select one team with the most rows\";")))
            .as("prose naming a table, with 'with' and 'select'").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java",
            "String head = \"select s.id from data_submission s where s.id = :id\";"))
            .as("a lowercase native query")
            .containsExactly(
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table data_submission outside its seam");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java",
            "String jpql = \"select activity from Activity activity join activity.project p where p.uid = :uid\";"))
            .as("JPQL, which names entities").isEmpty();
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java", String.join("\n",
            "String comma = \"SELECT 1 FROM feed.submission f, data_submission s WHERE f.id = s.id\";",
            "String using = \"DELETE FROM feed.submission f USING org_unit o WHERE f.org_unit_id = o.id\";",
            "String old = \"SELECT 1 FROM canonical_element_anchor a JOIN outbox_event e ON true\";")))
            .as("comma joins, USING, and the old tables")
            .containsExactly(
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table data_submission outside its seam",
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table org_unit outside its seam",
                "org/nmcpye/datarun/submission/feed/X.java: retiring relation canonical_element_anchor",
                "org/nmcpye/datarun/submission/feed/X.java: retiring relation outbox_event");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java", String.join("\n",
            "String alias = \"UPDATE data_submission s SET status = 'x' WHERE s.id = :id\";",
            "String only = \"update only outbox set status = 'x'\";",
            "String merge = \"MERGE INTO team t USING feed.submission f ON t.id = f.team_id \"",
            "    + \"WHEN MATCHED THEN DO NOTHING\";")))
            .as("UPDATE with an alias, UPDATE ONLY, and MERGE ... USING")
            .containsExactly(
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table data_submission outside its seam",
                "org/nmcpye/datarun/submission/feed/X.java: transitional table outbox (new code uses "
                    + "outbox.processing)",
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table team outside its seam");
        assertThat(sqlFindings("org/nmcpye/datarun/submission/feed/X.java",
            "log.warn(\"Delete from team {} refused\", id);"))
            .as("a known false positive: prose shaped like a query")
            .containsExactly(
                "org/nmcpye/datarun/submission/feed/X.java: system-of-record table team outside its seam");
    }

    // Fixtures for the tests above.

    /** Stands for new code: one leak of each kind, and one use of each seam it may use. */
    @SuppressWarnings("unused")
    static final class Leak {
        void leaks(TallCanonicalJdbcRepository tall, DataSubmission entity, CurrentUserDetails principal,
                   DataSubmissionUploadV1Dto payload, OutboxDto outboxRow) {
            new SubmissionCommand.Delete(null, "a uid");
        }

        void allowed(SubmissionCommandService commands, ValueType type, FailureShape shape) {
        }
    }

    /** Stands for code that stays and reaches into retiring code: one dependency listed, one not. */
    @SuppressWarnings("unused")
    static final class StayingLeak {
        EtlProperties lease;
        PivotService pivots;
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

    /**
     * A seam: exactly this class, or this package with its subpackages ({@code "x.."}), usable from new code in these
     * packages ({@code null}: any new code), and why.
     */
    record Seam(String target, String usedFrom, String why) {

        boolean isPackage() {
            return target.endsWith("..");
        }

        String packageName() {
            return target.substring(0, target.length() - 2);
        }

        boolean allows(JavaClass origin, JavaClass target) {
            boolean seam = isPackage() ? PackageMatcher.of(this.target).matches(target.getPackageName())
                : target.getName().equals(this.target);
            return seam && (usedFrom == null || PackageMatcher.of(usedFrom).matches(origin.getPackageName()));
        }
    }

    /** A neutral or transitional class that still depends on retiring code, and on what. */
    record Leftover(Class<?> origin, Class<?> dependency, String why) {

        boolean covers(JavaClass from, JavaClass target) {
            return isOrNestedIn(from, origin.getName()) && isOrNestedIn(target, dependency.getName());
        }
    }

    /** A listed breach: new code in this class (nested classes included) depends on that class, and why. */
    record Breach(String origin, String dependency, String why) {

        boolean covers(JavaClass from, JavaClass target) {
            return isOrNestedIn(from, origin) && isOrNestedIn(target, dependency);
        }
    }

    /** A seam's own SQL: the source (a file, or a package folder) where new code may name these tables, and why. */
    record SqlSeam(String source, Set<String> tables, boolean planned, String why) {
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

    /** The kind a class in this package would have by the package rows alone. */
    static Optional<Kind> kindOfPackage(String packageName) {
        return CLASSIFICATION.stream().filter(r -> !r.isClass() && r.specificity(packageName) >= 0)
            .max(Comparator.comparingInt(r -> r.specificity(packageName))).map(Row::kind);
    }

    static DescribedPredicate<JavaClass> ofKind(Kind kind) {
        return DescribedPredicate.describe(kind.label, c -> kindOf(c.getBaseComponentType()).orElse(null) == kind);
    }

    /** All old code that stays: everything neither new nor retiring. */
    static final DescribedPredicate<JavaClass> STAYS = ofKind(NEUTRAL).or(ofKind(TRANSITIONAL))
        .or(ofKind(SYSTEM_OF_RECORD)).or(ofKind(CLIENT_CONTRACT)).as("code that stays");

    /** The class a main source file declares, for the SQL scan to classify its file like its class. */
    private static JavaClass sourceClass(String path) {
        String name = path.substring(0, path.length() - ".java".length()).replace('/', '.');
        return MAIN.contain(name) ? MAIN.get(name) : MAIN.get(DataRunApiApp.class);
    }

    /**
     * Each dependency on code of the guarded kinds is a violation, reported with its location, unless a seam allows it
     * (where seams apply) or a breach lists it.
     */
    private static ArchCondition<JavaClass> dependOnGuardedCodeOnlyAsListed(Set<Kind> guarded, boolean throughSeams) {
        String kinds = guarded.stream().map(k -> k.label).sorted().collect(Collectors.joining(" or "));
        return new ArchCondition<>(throughSeams ? "depend on " + kinds + " only through the seams"
            : "depend on no " + kinds) {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    boolean allowed = (throughSeams && SEAMS.stream().anyMatch(seam -> seam.allows(origin, target)))
                        || BREACHES.stream().anyMatch(breach -> breach.covers(origin, target));
                    if (kindOf(target).filter(guarded::contains).isPresent() && !allowed) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** Each dependency on retiring code that no leftover lists is a violation, reported with its location. */
    private static ArchCondition<JavaClass> dependOnRetiringCodeOnlyAs(List<Leftover> leftovers) {
        return new ArchCondition<>("depend on retiring code only as a listed leftover") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    if (kindOf(target).orElse(null) == RETIRING
                        && leftovers.stream().noneMatch(leftover -> leftover.covers(origin, target))) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** Whether the class (nested classes included) depends on the other (nested classes included). */
    private static boolean occurs(String origin, String dependency) {
        return MAIN.stream().filter(c -> isOrNestedIn(c, origin))
            .flatMap(c -> c.getDirectDependenciesFromSelf().stream())
            .anyMatch(d -> isOrNestedIn(d.getTargetClass().getBaseComponentType(), dependency));
    }

    /** Whether the class is the named class or one nested in it. */
    private static boolean isOrNestedIn(JavaClass javaClass, String className) {
        return javaClass.getName().equals(className) || javaClass.getName().startsWith(className + "$");
    }

    private static List<String> details(ArchRule rule, JavaClasses classes) {
        return rule.evaluate(classes).getFailureReport().getDetails();
    }

    /** An optional double quote, escaped or not: Java source text holds quoted identifiers as \". */
    private static final String QUOTE = "(?:\\\\?\")?";
    /** A literal with the structure of a query, in any case. */
    private static final Pattern SQL_LIKE = Pattern.compile("(?is)\\bselect\\b.*?\\bfrom\\b|\\binsert\\s+into\\b"
        + "|\\bupdate\\s+(?:only\\s+)?\\S+(?:\\s+(?:as\\s+)?\\w+)?\\s+set\\b|\\bdelete\\s+from\\b"
        + "|\\bmerge\\s+into\\b.*?\\busing\\b|\\bwith\\s+\\w+\\s+as\\s*\\(");
    /** A relation's name, schema-qualified or not, quoted or not. */
    private static final String NAME =
        "(?:only\\s+)?(?:" + QUOTE + "(\\w+)" + QUOTE + "\\s*\\.\\s*)?" + QUOTE + "(\\w+)";
    /** A relation after an SQL word that takes one. */
    private static final Pattern RELATION =
        Pattern.compile("(?i)\\b(?:from|join|into|update|truncate|table|copy|using)\\s+" + NAME);
    /** A FROM list: from FROM to the next clause, closing parenthesis or end. */
    private static final Pattern FROM_LIST = Pattern.compile("(?is)\\bfrom\\s+(.*?)(?=\\b(?:where|join|left|right"
        + "|inner|full|cross|natural|group|order|limit|offset|having|union|except|intersect|returning|on|window|for)\\b"
        + "|\\)|;|$)");
    /** The relation an item of a FROM list starts with. */
    private static final Pattern LEADING = Pattern.compile("(?i)\\s*" + NAME);

    /** The relations a statement names: after the SQL words that take one, and every item of a FROM list. */
    static List<String[]> relations(String sql) {
        List<String[]> relations = new ArrayList<>();
        Matcher relation = RELATION.matcher(sql);
        while (relation.find()) {
            relations.add(new String[] {relation.group(1), relation.group(2)});
        }
        Matcher fromList = FROM_LIST.matcher(sql);
        while (fromList.find()) {
            String[] items = fromList.group(1).split(",");
            for (int i = 1; i < items.length; i++) {
                Matcher item = LEADING.matcher(items[i]);
                if (item.lookingAt()) {
                    relations.add(new String[] {item.group(1), item.group(2)});
                }
            }
        }
        return relations;
    }
}
