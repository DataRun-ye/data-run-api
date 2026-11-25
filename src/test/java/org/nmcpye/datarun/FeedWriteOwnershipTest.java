package org.nmcpye.datarun;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.submission.feed.publisher.DictionaryPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Only the feed's publisher ({@code submission.feed.publisher}) writes schemas {@code feed} and {@code feed_ops}
 * (feed build step F6): the feed is a published contract whose order and completeness rest on one writer taking one
 * lock (contract 8).
 *
 * <ul>
 *   <li>ArchUnit: nothing outside the publisher's package reaches into it. The runtime that applies outbox changes
 *       reaches the publisher only through the seam it implements ({@code outbox.processing.ChangeOutput} and
 *       {@code DeferredWork}), as Spring wires it.</li>
 *   <li>Text, which ArchUnit cannot see, scanned as a tripwire, not proof. In the main sources outside the publisher
 *       it flags: a schema-qualified feed relation in a string literal or text block, named or written; a search path
 *       set to the feed; a JPA mapping to either schema ({@code schema = "feed"}, as {@code @Table} and the other
 *       mapping annotations take it); and {@code withSchemaName("feed")}, as {@code SimpleJdbcInsert} and
 *       {@code SimpleJdbcCall} take it. In the resources, any feed relation or {@code schemaName="feed"} outside the
 *       feed's own changelogs ({@code config/liquibase/changelog/feed/}).</li>
 * </ul>
 * <p>What it cannot see: names split across literals or held in constants and assembled at run time
 * ({@code "feed" + "." + table}); SQL read from files outside the main resources; a schema given to JPA by
 * {@code orm.xml}, a naming strategy or a default schema setting; unqualified names reaching the feed through a search
 * path set by the connection's configuration or the database role. The two schemas are on no search path, so SQL
 * reaches them only in one of these ways or by naming them, which the scan flags.</p>
 */
class FeedWriteOwnershipTest {

    private static final String PUBLISHER = "org.nmcpye.datarun.submission.feed.publisher";
    private static final String PUBLISHER_SOURCES = "org/nmcpye/datarun/submission/feed/publisher/";
    /** The feed's own changelogs: the only resources that may name its relations. */
    private static final String RESOURCES_NAMED_IN = "config/liquibase/changelog/feed/";

    private static final JavaClasses MAIN = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("org.nmcpye.datarun");

    /** The publisher's entry points for other packages: none, since the runtime reaches it through the seam. */
    private static final Class<?>[] ENTRY_POINTS = {};

    static final ArchRule ONLY_THE_PUBLISHER_WRITES_THE_FEED = noClasses()
        .that().resideOutsideOfPackage(PUBLISHER + "..")
        .should().dependOnClassesThat(resideInAPackage(PUBLISHER + "..").and(not(belongToAnyOf(ENTRY_POINTS))))
        .because("only the publisher writes schemas feed and feed_ops, in order, under its lock (feed contract 8); "
            + "everything else reaches it through its entry points");

    /** An optional double quote, escaped or not: Java source text holds quoted identifiers as \\". */
    private static final String QUOTE = "(?:\\\\?\")?";
    /** A relation of the feed's schemas, qualified, quoted or not; never a property name such as datarun.feed.x. */
    private static final Pattern RELATION = Pattern.compile(
        "(?i)(?<![\\w.])" + QUOTE + "(feed(?:_ops)?)" + QUOTE + "\\s*\\.\\s*" + QUOTE + "(\\w+)");
    /** A literal that is just a relation, for SQL assembled from pieces. */
    private static final Pattern WHOLE_RELATION = Pattern.compile(
        "(?i)^\\s*" + QUOTE + "feed(?:_ops)?" + QUOTE + "\\s*\\.\\s*" + QUOTE + "\\w+" + QUOTE + "\\s*$");
    private static final Pattern WRITE = Pattern.compile("(?i)\\b(insert\\s+into|update|delete\\s+from|merge\\s+into"
        + "|truncate(?:\\s+table)?|copy|alter\\s+table|drop\\s+table|create\\s+(?:unlogged\\s+)?table)\\s+"
        + "(?:only\\s+)?(?:if\\s+(?:not\\s+)?exists\\s+)?" + QUOTE + "(feed(?:_ops)?)" + QUOTE + "\\s*\\.");
    private static final Pattern SEARCH_PATH = Pattern.compile("(?i)search_path.*\\bfeed(?:_ops)?\\b");
    /** A JPA mapping to either schema: {@code schema = "feed"} in {@code @Table} and the other mapping annotations. */
    private static final Pattern JPA_SCHEMA = Pattern.compile("\\bschema\\s*=\\s*\"(feed(?:_ops)?)\"");
    /** {@code withSchemaName("feed")}, as {@code SimpleJdbcInsert} and {@code SimpleJdbcCall} take it. */
    private static final Pattern JDBC_SCHEMA =
        Pattern.compile("\\bwithSchemaName\\s*\\(\\s*\"(feed(?:_ops)?)\"\\s*\\)");
    /** Liquibase's {@code schemaName="feed"}, as structured changes take it. */
    private static final Pattern CHANGELOG_SCHEMA = Pattern.compile("\\bschemaName\\s*=\\s*\"(feed(?:_ops)?)\"");
    /** "feed." ends many an English sentence, so a relation counts only beside an SQL word or as a whole literal. */
    private static final Pattern SQL_WORD = Pattern.compile("(?i)\\b(select|insert|update|delete|merge|truncate|copy"
        + "|from|into|join|where|set|values|returning|alter|drop|create|table|grant|revoke|lock)\\b");

    @Test
    void nothingOutsideThePublisherReachesIntoIt() {
        ONLY_THE_PUBLISHER_WRITES_THE_FEED.check(MAIN);
    }

    /** The rule can fail: a class outside the publisher that calls it is caught. */
    @Test
    void aClassOutsideThePublisherThatReachesItBreaksTheRule() {
        JavaClasses withABackDoor = new ClassFileImporter().importClasses(BackDoor.class, DictionaryPublisher.class);

        assertThat(ONLY_THE_PUBLISHER_WRITES_THE_FEED.evaluate(withABackDoor).getFailureReport().getDetails())
            .isNotEmpty()
            .allSatisfy(line -> assertThat(line).contains(BackDoor.class.getName()));
    }

    /** A rewrite of the dictionary from outside the publisher, for the test above only. */
    @SuppressWarnings("unused")
    static class BackDoor {
        boolean rewrite(DictionaryPublisher publisher) {
            return publisher.publishIfChanged();
        }
    }

    /**
     * A tripwire, not proof: every string literal and text block of the main sources, and every line of the main
     * resources, is read for the feed's relations.
     */
    @Test
    void noSqlTextOutsideThePublisherNamesOrWritesTheFeed() throws IOException {
        List<String> outside = new ArrayList<>();
        int allowed = 0;
        Path sources = Path.of("src", "main", "java");
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String finding : sourceFindings(relative(sources, file), Files.readString(file))) {
                    if (finding.startsWith("allowed ")) {
                        allowed++;
                    } else {
                        outside.add(finding);
                    }
                }
            }
        }
        Path resources = Path.of("src", "main", "resources");
        try (Stream<Path> files = Files.walk(resources)) {
            for (Path file : files.filter(path -> path.toString().matches(".*\\.(xml|ya?ml|sql|properties|json)$"))
                .toList()) {
                outside.addAll(resourceFindings(relative(resources, file), Files.readString(file)));
            }
        }
        assertThat(allowed).as("the scan finds the publisher's SQL that does exist").isPositive();
        assertThat(outside)
            .as("SQL text naming or writing schema feed or feed_ops outside the publisher (a tripwire, not proof)")
            .isEmpty();
    }

    @Test
    void theSqlScanFlagsWhatItShould() {
        assertThat(sourceFindings("org/nmcpye/datarun/web/X.java", String.join("\n",
            "// DELETE FROM feed.form_field in a comment",
            "String a = \"DELETE FROM feed.form_field\";",
            "String b = \"\"\"",
            "    INSERT INTO \"feed\".\"submission\" (id)",
            "    VALUES (?)\"\"\";",
            "String c = \"SELECT count(*) FROM feed_ops.pending\";",
            "String d = \"SET LOCAL search_path TO feed, public\";",
            "String e = \"TRUNCATE TABLE feed . submission_row\";",
            "String f = \"UPDATE \\\"feed\\\".\\\"freshness\\\" SET dead = 0\";",
            "String g = \"feed.submission\";")))
            .containsExactly(
                "outside org/nmcpye/datarun/web/X.java: names feed.form_field in \"DELETE FROM feed.form_field\"",
                "outside org/nmcpye/datarun/web/X.java: writes feed in \"DELETE FROM feed.form_field\"",
                "outside org/nmcpye/datarun/web/X.java: names feed.submission in \"\n    INSERT INTO \"feed\".\"submission\" "
                    + "(id)\n    VALUES (?)\"",
                "outside org/nmcpye/datarun/web/X.java: writes feed in \"\n    INSERT INTO \"feed\".\"submission\" (id)\n"
                    + "    VALUES (?)\"",
                "outside org/nmcpye/datarun/web/X.java: names feed_ops.pending in \"SELECT count(*) FROM feed_ops.pending\"",
                "outside org/nmcpye/datarun/web/X.java: sets a search path to the feed in \"SET LOCAL search_path TO feed, "
                    + "public\"",
                "outside org/nmcpye/datarun/web/X.java: names feed.submission_row in \"TRUNCATE TABLE feed . "
                    + "submission_row\"",
                "outside org/nmcpye/datarun/web/X.java: writes feed in \"TRUNCATE TABLE feed . submission_row\"",
                "outside org/nmcpye/datarun/web/X.java: names feed.freshness in \"UPDATE \\\"feed\\\".\\\"freshness\\\" SET "
                    + "dead = 0\"",
                "outside org/nmcpye/datarun/web/X.java: writes feed in \"UPDATE \\\"feed\\\".\\\"freshness\\\" SET dead = 0\"",
                "outside org/nmcpye/datarun/web/X.java: names feed.submission in \"feed.submission\"");
        assertThat(sourceFindings(PUBLISHER_SOURCES + "X.java", "String a = \"DELETE FROM feed.form_field\";"))
            .allMatch(finding -> finding.startsWith("allowed "))
            .hasSize(2);
        assertThat(sourceFindings("org/nmcpye/datarun/submission/feed/X.java", String.join("\n",
            "String property = \"datarun.feed.reader-role\";",
            "String schema = \"SELECT 1 FROM pg_namespace n WHERE n.nspname = 'feed'\";",
            "String words = \"Published the feed. Its records follow\";",
            "String other = \"SELECT * FROM newsfeed.items\";")))
            .as("a property name, the schema's name as a value, prose and another schema are not the feed's relations")
            .isEmpty();
        assertThat(resourceFindings("config/application.yml", "query: UPDATE feed.freshness SET backlog = 0"))
            .containsExactly("outside config/application.yml:1: names feed.freshness in \"query: UPDATE feed.freshness "
                + "SET backlog = 0\"");
        assertThat(resourceFindings("config/liquibase/changelog/feed/x.xml", "DROP TABLE feed.freshness")).isEmpty();
        assertThat(resourceFindings("config/liquibase/changelog/etl/x.xml", String.join("\n",
            "<sql>UPDATE feed.freshness SET dead = 0</sql>",
            "<insert schemaName=\"feed_ops\" tableName=\"pending\">")))
            .as("another changelog, in SQL or as a structured change").containsExactly(
                "outside config/liquibase/changelog/etl/x.xml:1: names feed.freshness in \"<sql>UPDATE feed.freshness "
                    + "SET dead = 0</sql>\"",
                "outside config/liquibase/changelog/etl/x.xml:2: names schema feed_ops in \"<insert "
                    + "schemaName=\"feed_ops\" tableName=\"pending\">\"");
    }

    @Test
    void theScanFlagsMappingsToTheFeedsSchemas() {
        String source = String.join("\n",
            "// @Table(schema = \"feed\") in a comment",
            "/* new SimpleJdbcInsert(ds).withSchemaName(\"feed\") in a block comment */",
            "@Table(name = \"submission\", schema = \"feed\")",
            "@Table(schema=\"feed_ops\", name = \"pending\")",
            "SimpleJdbcInsert insert = new SimpleJdbcInsert(ds).withSchemaName( \"feed\" ).withTableName(\"x\");",
            "String text = \"schema = \\\"feed\\\" said in a literal\";");
        assertThat(sourceFindings("org/nmcpye/datarun/web/X.java", source)).containsExactly(
            "outside org/nmcpye/datarun/web/X.java: maps to schema feed in \"schema = \"feed\"\"",
            "outside org/nmcpye/datarun/web/X.java: maps to schema feed_ops in \"schema=\"feed_ops\"\"",
            "outside org/nmcpye/datarun/web/X.java: maps to schema feed in \"withSchemaName( \"feed\" )\"");
        assertThat(sourceFindings(PUBLISHER_SOURCES + "X.java", source)).hasSize(3)
            .allMatch(finding -> finding.startsWith("allowed "));
        assertThat(withoutComments("a // b \"c\"\n\"d // e\" /* f */ g")).isEqualTo("a \n\"d // e\"  g");
    }

    /** What a Java source says about the feed's relations: one entry each, starting "allowed " or "outside ". */
    static List<String> sourceFindings(String path, String source) {
        String where = path.startsWith(PUBLISHER_SOURCES) ? "allowed " : "outside ";
        List<String> findings = new ArrayList<>();
        for (String literal : JavaLiterals.strings(source)) {
            List<String> seen = new ArrayList<>();
            boolean sqlLike = SQL_WORD.matcher(literal).find() || WHOLE_RELATION.matcher(literal).matches();
            Matcher relation = RELATION.matcher(literal);
            while (sqlLike && relation.find()) {
                String name = relation.group(1).toLowerCase(Locale.ROOT) + "." + relation.group(2);
                if (!seen.contains(name)) {
                    seen.add(name);
                    findings.add(where + path + ": names " + name + " in \"" + literal + "\"");
                }
            }
            Matcher write = WRITE.matcher(literal);
            while (write.find()) {
                findings.add(where + path + ": writes " + write.group(2).toLowerCase(Locale.ROOT) + " in \"" + literal
                    + "\"");
            }
            if (SEARCH_PATH.matcher(literal).find()) {
                findings.add(where + path + ": sets a search path to the feed in \"" + literal + "\"");
            }
        }
        String code = withoutComments(source);
        for (Pattern schema : List.of(JPA_SCHEMA, JDBC_SCHEMA)) {
            Matcher mapping = schema.matcher(code);
            while (mapping.find()) {
                findings.add(where + path + ": maps to schema " + mapping.group(1) + " in \"" + mapping.group() + "\"");
            }
        }
        return findings;
    }

    /** Java source without its comments, string literals and text blocks kept as they are. */
    static String withoutComments(String source) {
        StringBuilder code = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            if (source.startsWith("//", i)) {
                int end = source.indexOf('\n', i);
                i = end < 0 ? source.length() : end;
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
            } else if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                end = end < 0 ? source.length() : end + 3;
                code.append(source, i, end);
                i = end;
            } else if (source.charAt(i) == '"' || source.charAt(i) == '\'') {
                char quote = source.charAt(i);
                int j = i + 1;
                while (j < source.length() && source.charAt(j) != quote) {
                    j += source.charAt(j) == '\\' ? 2 : 1;
                }
                int end = Math.min(j + 1, source.length());
                code.append(source, i, end);
                i = end;
            } else {
                code.append(source.charAt(i));
                i++;
            }
        }
        return code.toString();
    }

    /** Lines of a resource that name the feed's relations or schemas outside the feed's own changelogs. */
    static List<String> resourceFindings(String path, String text) {
        if (path.startsWith(RESOURCES_NAMED_IN)) {
            return List.of();
        }
        List<String> findings = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher relation = RELATION.matcher(lines[i]);
            while (SQL_WORD.matcher(lines[i]).find() && relation.find()) {
                findings.add("outside " + path + ":" + (i + 1) + ": names " + relation.group(1).toLowerCase(Locale.ROOT)
                    + "." + relation.group(2) + " in \"" + lines[i].strip() + "\"");
            }
            Matcher schema = CHANGELOG_SCHEMA.matcher(lines[i]);
            while (schema.find()) {
                findings.add("outside " + path + ":" + (i + 1) + ": names schema " + schema.group(1) + " in \""
                    + lines[i].strip() + "\"");
            }
        }
        return findings;
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
