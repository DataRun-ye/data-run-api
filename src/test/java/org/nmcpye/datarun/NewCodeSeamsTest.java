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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * New code does not couple to old structures (the owner's rule of 2026-11-25, as engineering refined it). The map
 * this test encodes, with the reasons, is {@code docs/agent-context/legacy-seams.md}.
 *
 * <ul>
 *   <li>(a) <b>Retiring derived structures</b> ({@link #RETIRING}): no dependency either way. New code never names
 *       their classes, and their classes never name new code, so neither can share the other's transaction, schedule
 *       or failure.</li>
 *   <li>(b) <b>Systems of record</b> ({@link #SYSTEM_OF_RECORD}): new code depends on their classes only through the
 *       seams in {@link #SEAMS}, each listed exactly (a nested class is not a seam unless listed) with a reason.</li>
 *   <li>(c) <b>Client contracts</b> ({@link #CLIENT_CONTRACT}): new code never depends on v1 resources, their DTOs,
 *       mappers or the v1 error vocabulary.</li>
 *   <li>Every main class is classified exactly once, so a new package cannot escape the rules unclassified.</li>
 *   <li>SQL text in new code, which ArchUnit cannot see, is scanned as a tripwire, not proof: it names no retiring
 *       relation, and system-of-record tables only in the seam that owns the read ({@link #SEAM_SQL}).</li>
 * </ul>
 * <p>No violation is accepted or frozen: an exception is a listed seam with its reason, or a change of this map.</p>
 */
class NewCodeSeamsTest {

    private static final String ROOT = "org.nmcpye.datarun";

    private static final JavaClasses MAIN = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(ROOT);

    // The classification: every main class is in exactly one of these five.

    /** Where new code lives: the normalizer and the feed, v2, the journal, conflict detection. */
    static final DescribedPredicate<JavaClass> NEW_CODE = packages(
        "submission..", "web.v2..", "jpa.datasubmission.revision..", "jpa.datasubmission.conflict..")
        .as("new code");

    /**
     * Retiring derived structures: tall, events, dimensions and pivots, and the ETL that writes them; the outbox, which
     * only the ETL consumes and whose payload copies {@code form_data}; {@code canonical_element}, its generator and
     * its types, which only the ETL and the pivots read; the pivot route.
     */
    static final DescribedPredicate<JavaClass> RETIRING = packages(
        "etl..", "outbox..", "jpa.datatemplategenerator..", "jpa.etl")
        .or(partOf(CanonicalElement.class, CanonicalElementRepository.class, MetadataUpsertService.class,
            TemplateElement.class, SemanticType.class, DataType.class, PivotController.class))
        .as("retiring derived code");

    /**
     * Contracts with installed clients and the admin's tools: the v1 routes and the generic resources, their payloads
     * and mappers, the v1 query language and the v1 error vocabulary (codes the upload summary sends to phones).
     */
    static final DescribedPredicate<JavaClass> CLIENT_CONTRACT = packages(
        "web.rest..", "apiquery..", "common.feedback..",
        "jpa.assignment.dto..", "jpa.assignment.mapper..", "jpa.datatemplate.dto..", "jpa.datatemplate.mapper..",
        "jpa.user.dto..", "jpa.user.mapper..", "jpa.userrefreshtoken.dto..", "service.dto..")
        .or(partOf(EntitySaveSummaryVM.class, JpaIdentifiableOperationVm.class, BaseMapper.class, BaseDto.class,
            ErrorCodeException.class, IllegalQueryException.class, FormSubmissionDataUtil.class))
        .and(not(RETIRING))
        .as("client contract code");

    /**
     * Systems of record still in use, with their old internals: submissions, held uploads and Reference lists; form
     * templates and versions, data elements and option sets; assignments, teams, org units and activities; accounts
     * and access scope; the generic entity bases, services, repositories and auditing they are built on.
     */
    static final DescribedPredicate<JavaClass> SYSTEM_OF_RECORD = packages(
        "jpa.datasubmission", "jpa.datasubmission.command..", "jpa.datasubmission.repository..",
        "jpa.datasubmission.service..", "jpa.datasubmission.validation..", "jpa.datasubmissionbatching..",
        "jpa.intake..", "jpa.reference..",
        "jpa.datatemplate..", "datatemplateelement..", "datatemplateprocessor..", "jpa.dataelement..", "jpa.option..",
        "jpa.assignment..", "jpa.team..", "jpa.orgunit..", "jpa.orgunitgroup..", "jpa.orgunitgroupset..",
        "jpa.oulevel..", "jpa.activity..", "jpa.project..",
        "jpa.user..", "jpa.userauthority..", "jpa.usegroup..", "jpa.userrefreshtoken..", "jpa.accessfilter..",
        "jpa.migration..", "security..", "userdetail..", "service..",
        "jpa", "jpa.common..", "jpa.auditing..", "common", "common.enumeration..", "common.repository..",
        "common.translation..")
        .and(not(RETIRING)).and(not(CLIENT_CONTRACT))
        .as("system-of-record code");

    /** Neutral infrastructure any code may use: configuration, activation switches, request ids, failure shapes. */
    static final DescribedPredicate<JavaClass> NEUTRAL = resideInAPackage(ROOT)
        .or(packages("config..", "aop..", "management..", "web.filter.."))
        .or(partOf(FailureShape.class, DatabaseFailures.class, CodeGenerator.class, UuidUtils.class))
        .as("neutral infrastructure");

    // (b) The seams.

    /** A system-of-record class new code may use: exactly this class, from these packages (null: any new code). */
    record Seam(Class<?> type, String usedFrom, String reason) {

        boolean allows(JavaClass origin, JavaClass target) {
            return target.getName().equals(type.getName())
                && (usedFrom == null || PackageMatcher.of(usedFrom).matches(origin.getPackageName()));
        }
    }

    /**
     * The only system-of-record classes new code may depend on. The normalizer ({@code submission.normalized}) and the
     * journal ({@code jpa.datasubmission.revision}) are seams too, but new code themselves, so not listed.
     */
    static final List<Seam> SEAMS = List.of(
        new Seam(SubmissionCommandService.class, null,
            "the single write path; its command variants are listed one by one, and none is yet: each of today's "
                + "carries a phone payload, the entity, a v1 request or a uid"),
        new Seam(SubmissionActor.class, null,
            "who a write-path command acts as; SubmissionActor.current() builds it from the request"),
        new Seam(ValueType.class, null,
            "the stored value-type vocabulary, carried by the normalizer's field definitions and values"),
        new Seam(CurrentUserDetails.class, ROOT + ".web.v2.caller..",
            "ApiCaller adapts the request's principal to its user id and authorities, and nothing else"),
        new Seam(DomainUserDetailsService.class, ROOT + ".web.v2.config..",
            "the v2 chain resolves the bearer token's account as v1 does: authentication, not scope"));

    // The rules, built from their predicates so that the test below can show each of them failing.

    static ArchRule newCodeDependsOnNoRetiringCode(DescribedPredicate<JavaClass> newCode) {
        return noClasses().that(newCode).should().dependOnClassesThat(RETIRING)
            .because("retiring derived structures get no dependency: no read, no write, no import (rule a)");
    }

    static ArchRule retiringCodeDependsOnNoNewCode(DescribedPredicate<JavaClass> retiring,
                                                   DescribedPredicate<JavaClass> newCode) {
        return noClasses().that(retiring).should().dependOnClassesThat(newCode)
            .because("retiring code that calls new code shares its transaction, its schedule and its failures with it "
                + "(rule a)");
    }

    static ArchRule newCodeReachesSystemsOfRecordOnlyThroughSeams(DescribedPredicate<JavaClass> newCode) {
        return classes().that(newCode).should(dependOnSystemOfRecordOnlyThroughTheSeams())
            .because("systems of record are reached through one named seam each, never through their entities, "
                + "repositories or generic services (rule b)");
    }

    static ArchRule newCodeDependsOnNoClientContract(DescribedPredicate<JavaClass> newCode) {
        return noClasses().that(newCode).should().dependOnClassesThat(CLIENT_CONTRACT)
            .because("installed clients' contracts are adapted at the edge; their shapes, uids and error codes never "
                + "travel inward (rule c)");
    }

    @Test
    void newCodeTakesNoDependencyOnRetiringCode() {
        newCodeDependsOnNoRetiringCode(NEW_CODE).check(MAIN);
    }

    @Test
    void retiringCodeTakesNoDependencyOnNewCode() {
        retiringCodeDependsOnNoNewCode(RETIRING, NEW_CODE).check(MAIN);
    }

    @Test
    void newCodeReachesSystemsOfRecordOnlyThroughTheirSeams() {
        newCodeReachesSystemsOfRecordOnlyThroughSeams(NEW_CODE).check(MAIN);
    }

    @Test
    void newCodeTakesNoDependencyOnClientContracts() {
        newCodeDependsOnNoClientContract(NEW_CODE).check(MAIN);
    }

    /** Unclassified code would escape every rule; code classified twice would make the map ambiguous. */
    @Test
    void everyMainClassIsClassifiedExactlyOnce() {
        List<DescribedPredicate<JavaClass>> kinds = List.of(NEW_CODE, RETIRING, SYSTEM_OF_RECORD, CLIENT_CONTRACT,
            NEUTRAL);
        List<String> wrong = MAIN.stream()
            .filter(c -> !c.getSimpleName().equals("package-info"))
            .filter(c -> kinds.stream().filter(kind -> kind.test(c)).count() != 1)
            .map(c -> c.getName() + ": " + kinds.stream().filter(kind -> kind.test(c))
                .map(DescribedPredicate::getDescription).toList())
            .sorted().toList();
        assertThat(MAIN.size()).as("classes imported").isGreaterThan(300);
        assertThat(wrong).as("main classes not classified exactly once (add the package to one kind)").isEmpty();
    }

    /** A seam is a system-of-record class that exists, with a reason; its scope is new code. */
    @Test
    void everySeamIsASystemOfRecordClass() {
        for (Seam seam : SEAMS) {
            JavaClass type = MAIN.get(seam.type());
            assertThat(SYSTEM_OF_RECORD.test(type)).as(seam.type().getName() + " is system-of-record code").isTrue();
            assertThat(seam.reason()).as(seam.type().getName() + " has a reason").isNotBlank();
        }
    }

    /** The rules can fail: each kind of leak is caught, and what the seams allow is not. */
    @Test
    void theRulesCatchALeak() {
        JavaClasses classes = new ClassFileImporter().importClasses(Leak.class, EtlLeak.class);
        DescribedPredicate<JavaClass> leak = belongToAnyOf(Leak.class);

        assertThat(details(newCodeDependsOnNoRetiringCode(leak), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("TallCanonicalJdbcRepository"));
        assertThat(details(retiringCodeDependsOnNoNewCode(belongToAnyOf(EtlLeak.class), NEW_CODE), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("DictionaryPublisher"));
        assertThat(details(newCodeReachesSystemsOfRecordOnlyThroughSeams(leak), classes))
            .anySatisfy(line -> assertThat(line).contains("DataSubmission>"))
            .anySatisfy(line -> assertThat(line).contains("SubmissionCommand$Delete"))
            .anySatisfy(line -> assertThat(line).contains("CurrentUserDetails"))
            .noneSatisfy(line -> assertThat(line).containsAnyOf("SubmissionCommandService>", "ValueType"));
        assertThat(details(newCodeDependsOnNoClientContract(leak), classes))
            .isNotEmpty().allSatisfy(line -> assertThat(line).contains("DataSubmissionUploadV1Dto"));
    }

    private static List<String> details(ArchRule rule, JavaClasses classes) {
        return rule.evaluate(classes).getFailureReport().getDetails();
    }

    /** Stands for new code in the test above: one leak of each kind, and one use of each seam it may use. */
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

    // SQL text, which ArchUnit cannot see. Scanned as a tripwire, not proof: SQL built at run time is not seen.

    /** The sources of new code. */
    private static final List<String> NEW_CODE_SOURCES = List.of(
        "org/nmcpye/datarun/submission/", "org/nmcpye/datarun/web/v2/",
        "org/nmcpye/datarun/jpa/datasubmission/revision/", "org/nmcpye/datarun/jpa/datasubmission/conflict/");

    /** Retiring relations: every relation of schemas analytics (tall, events, dimensions) and pivot, and these. */
    private static final Set<String> RETIRING_SCHEMAS = Set.of("analytics", "pivot");
    private static final Set<String> RETIRING_TABLES = Set.of(
        "canonical_element", "template_element", "outbox", "outbox_processing", "etl_runs", "error_event");

    /** System-of-record tables (schema public). */
    private static final Set<String> SYSTEM_OF_RECORD_TABLES = Set.of(
        "data_submission", "held_submission", "go_live_rule", "reference_entry",
        "data_template", "data_template_version", "data_element", "option_set", "option_value", "option_group",
        "option_group_set", "assignment", "team", "team_user", "team_managed_teams", "activity", "project", "org_unit",
        "ou_level", "org_unit_group", "org_unit_group_members", "org_unit_groupset", "org_unit_groupset_org_unit_group",
        "app_user", "app_user_authority", "app_authority", "user_group", "refresh_token", "app_entity_audit_event");

    /**
     * The seams' own SQL, by source path prefix: the normalizer reads form definitions, and the journal copies (and on
     * {@code feat/review-s2} reads) the stored submission row.
     */
    static final Map<String, Set<String>> SEAM_SQL = Map.of(
        "org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java",
        Set.of("data_template", "data_template_version", "data_element", "option_set"),
        "org/nmcpye/datarun/jpa/datasubmission/revision/", Set.of("data_submission"));

    /** An optional double quote, escaped or not: Java source text holds quoted identifiers as \". */
    private static final String QUOTE = "(?:\\\\?\")?";
    /** A relation after an SQL word that takes one, schema-qualified or not. */
    private static final Pattern RELATION = Pattern.compile("(?i)\\b(?:from|join|into|update|truncate|table|copy)\\s+"
        + "(?:only\\s+)?(?:" + QUOTE + "(\\w+)" + QUOTE + "\\s*\\.\\s*)?" + QUOTE + "(\\w+)");

    @Test
    void sqlInNewCodeNamesNoRetiringRelationAndSystemsOfRecordOnlyInTheirSeams() throws IOException {
        List<String> findings = new ArrayList<>();
        int scanned = 0;
        Path sources = Path.of("src", "main", "java");
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String path = sources.relativize(file).toString().replace('\\', '/');
                if (NEW_CODE_SOURCES.stream().anyMatch(path::startsWith)) {
                    scanned++;
                    findings.addAll(sqlFindings(path, Files.readString(file)));
                }
            }
        }
        assertThat(scanned).as("new code's sources found").isPositive();
        assertThat(findings).as("SQL text in new code naming a retiring relation, or a system-of-record table "
            + "outside its seam (a tripwire, not proof: SQL built at run time is not seen)").isEmpty();
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
            "String claim = \"UPDATE \\\"outbox\\\" SET status = 'x'\";")))
            .containsExactly(
                "org/nmcpye/datarun/web/v2/X.java: retiring relation analytics.tall_canonical",
                "org/nmcpye/datarun/web/v2/X.java: retiring relation outbox");
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

    /** What a source of new code names: one finding per retiring relation, and per table outside its seam. */
    static List<String> sqlFindings(String path, String source) {
        List<String> findings = new ArrayList<>();
        for (String literal : SubmissionWriteOwnershipTest.stringLiterals(source)) {
            Matcher relation = RELATION.matcher(literal);
            while (relation.find()) {
                String schema = relation.group(1) == null ? null : relation.group(1).toLowerCase(Locale.ROOT);
                String table = relation.group(2).toLowerCase(Locale.ROOT);
                if (schema != null && RETIRING_SCHEMAS.contains(schema)) {
                    findings.add(path + ": retiring relation " + schema + "." + table);
                } else if ((schema == null || schema.equals("public")) && RETIRING_TABLES.contains(table)) {
                    findings.add(path + ": retiring relation " + table);
                } else if ((schema == null || schema.equals("public")) && SYSTEM_OF_RECORD_TABLES.contains(table)
                    && SEAM_SQL.entrySet().stream()
                    .noneMatch(seam -> path.startsWith(seam.getKey()) && seam.getValue().contains(table))) {
                    findings.add(path + ": system-of-record table " + table + " outside its seam");
                }
            }
        }
        return findings.stream().distinct().toList();
    }

    // Helpers.

    /** Classes in these packages, relative to the root: "jpa" is that package alone, "jpa.." with its subpackages. */
    private static DescribedPredicate<JavaClass> packages(String... relative) {
        return resideInAnyPackage(Arrays.stream(relative).map(p -> ROOT + "." + p).toArray(String[]::new));
    }

    /** These classes, with their nested classes and their JPA metamodel classes ({@code CanonicalElement_}). */
    private static DescribedPredicate<JavaClass> partOf(Class<?>... types) {
        Set<String> names = Arrays.stream(types).map(Class::getName).collect(Collectors.toSet());
        return DescribedPredicate.describe("part of " + names, c -> {
            String top = c.getName().split("\\$", 2)[0];
            return names.contains(top) || (top.endsWith("_") && names.contains(top.substring(0, top.length() - 1)));
        });
    }

    /** Each dependency on system-of-record code that no seam allows is a violation, reported with its location. */
    private static ArchCondition<JavaClass> dependOnSystemOfRecordOnlyThroughTheSeams() {
        return new ArchCondition<>("depend on system-of-record code only through the seams") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    if (SYSTEM_OF_RECORD.test(target)
                        && SEAMS.stream().noneMatch(seam -> seam.allows(origin, target))) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }
}
