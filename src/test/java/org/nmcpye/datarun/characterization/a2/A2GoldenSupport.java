package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;
import org.nmcpye.datarun.jpa.intake.GoLiveRule;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Shared harness for the A2 characterization goldens: the full application on the Testcontainers PostgreSQL that
 * {@link IntegrationTest} starts, the real Liquibase changelog, and MockMvc through the production filter chain
 * with real access tokens. Each scenario records its requests, the responses and, after every step, a snapshot of
 * the rows a submission write can touch ({@code data_submission}, {@code outbox}, {@code held_submission},
 * {@code reference_entry}), then compares the normalized document with its golden file ({@link A2Golden}).
 * <p>
 * The ETL never polls in these tests: {@code AsyncConfiguration}, which carries {@code @EnableScheduling}, is
 * excluded from the test profiles, so outbox rows stay {@code pending} and the goldens pin that.
 */
@IntegrationTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class A2GoldenSupport {

    private static final List<String> SERVER_TIMES = List.of(
        "created_date", "last_modified_date", "deleted_at", "created_at", "received_at", "decided_at",
        "held_last_modified_date");
    private static final List<String> EPOCH_TIMES = List.of(
        "start_entry_time", "finished_entry_time", "started_at", "live_from");
    private static final List<String> SERIALS = List.of("serial_number", "submission_serial_number");
    private static final List<String> JSON_COLUMNS = List.of("form_data", "payload");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected NamedParameterJdbcTemplate jdbc;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected GoLiveRuleRepository goLiveRules;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    protected String jwtKey;

    protected final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @BeforeAll
    void seedConfiguration() throws Exception {
        A2Seed.ensure(jdbc, transactionManager, mvc, jwtKey);
    }

    @BeforeEach
    void cleanBefore() {
        clean();
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    /** Removes what scenarios write; the seeded configuration stays. */
    protected void clean() {
        inTransaction(() -> {
            MapSqlParameterSource scope = scope();
            jdbc.update("""
                DELETE FROM outbox WHERE submission_uid LIKE 'a2%'
                   OR submission_id IN (SELECT id FROM data_submission WHERE assignment_uid IN (:assignments))""", scope);
            jdbc.update("DELETE FROM data_submission WHERE uid LIKE 'a2%' OR assignment_uid IN (:assignments)", scope);
            jdbc.update("DELETE FROM held_submission WHERE uid LIKE 'a2%' OR assignment_uid IN (:assignments)", scope);
            jdbc.update("""
                DELETE FROM reference_entry WHERE created_by <> 'a2-seed'
                   AND org_unit_id IN (SELECT id FROM org_unit WHERE uid IN (:orgUnits))""", scope);
            jdbc.update("DELETE FROM go_live_rule WHERE activity_uid = :activity", scope);
        });
    }

    protected void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private static MapSqlParameterSource scope() {
        return new MapSqlParameterSource()
            .addValue("assignments", A2Seed.ASSIGNMENTS)
            .addValue("orgUnits", A2Seed.ORG_UNITS)
            .addValue("activity", A2Seed.ACTIVITY);
    }

    protected Scenario scenario(String id, String title) {
        return new Scenario(id, title);
    }

    /** One scenario: its steps, and what normalization needs to know about them. */
    protected final class Scenario {
        private final String id;
        private final String title;
        private final ArrayNode steps = JsonNodeFactory.instance.arrayNode();
        private final Map<String, String> placeholders = new LinkedHashMap<>(A2Seed.placeholders());
        private final Map<Object, String> serials = new LinkedHashMap<>();
        private final Set<String> requestValues = new HashSet<>();
        private final Set<String> summarized = new HashSet<>();
        private final Map<String, String> sortedArrays = new LinkedHashMap<>();
        private JsonNode lastBody = NullNode.getInstance();
        private int lastStatus;

        private Scenario(String id, String title) {
            this.id = id;
            this.title = title;
        }

        /** An upload as the given phone generation sends it. */
        public Scenario upload(String label, Group group, String login, Item... items) {
            return send(label, group, "POST", group.route(), login, group.headers(), A2Payloads.body(items));
        }

        /** A practice upload (G3 only). */
        public Scenario practice(String label, String login, Item... items) {
            return send(label, Group.G3, "POST", A2Payloads.BULK_PRACTICE, login, Group.G3.headers(),
                A2Payloads.body(items));
        }

        /** Any request that no phone makes in this shape (admin calls, API-shaped uploads). */
        public Scenario call(String label, String method, String path, String login, Object body) {
            return send(label, null, method, path, login, Map.of(), body);
        }

        public Scenario call(String label, String method, String path, String login, Map<String, String> headers,
                             Object body) {
            return send(label, null, method, path, login, headers, body);
        }

        /** A change made outside the API (a correction by SQL, a revoked grant), recorded with its effect. */
        public Scenario sql(String label, String sql, Map<String, ?> params) {
            inTransaction(() -> jdbc.update(sql, params));
            ObjectNode step = JsonNodeFactory.instance.objectNode();
            step.put("step", steps.size() + 1);
            step.put("label", label);
            step.put("sql", sql);
            step.set("db", snapshot());
            steps.add(step);
            return this;
        }

        /** Puts a go-live rule for the A2 campaign and names its random uid. */
        public Scenario goLive(String label, String orgUnit, String form, String liveFrom) {
            GoLiveRule rule = goLiveRules.put(A2Seed.ACTIVITY, orgUnit, form, Instant.parse(liveFrom), null, "a2-test");
            placeholder(rule.uid(), "rule " + label);
            ObjectNode step = JsonNodeFactory.instance.objectNode();
            step.put("step", steps.size() + 1);
            step.put("label", "go-live rule " + label);
            step.put("orgUnit", orgUnit);
            step.put("form", form);
            step.put("liveFrom", liveFrom);
            steps.add(step);
            return this;
        }

        /** Work done outside HTTP (a direct service call), recorded with its result and its effect. */
        public Scenario action(String label, Supplier<?> work) {
            Object result = work.get();
            ObjectNode step = JsonNodeFactory.instance.objectNode();
            step.put("step", steps.size() + 1);
            step.put("label", label);
            step.set("result", json.valueToTree(result));
            step.set("db", snapshot());
            steps.add(step);
            return this;
        }

        /** Records arrays under this key in response bodies as their size only (long catalog pages). */
        public Scenario summarize(String key) {
            summarized.add(key);
            return this;
        }

        /** Records arrays under this key in response bodies sorted by a field, where order is no contract. */
        public Scenario sortBy(String key, String field) {
            sortedArrays.put(key, field);
            return this;
        }

        /** Names a value that is random per run (a go-live rule uid, a uid the server made). */
        public Scenario placeholder(String value, String name) {
            placeholders.put(value, "<" + name + ">");
            return this;
        }

        public JsonNode lastBody() {
            return lastBody;
        }

        public int lastStatus() {
            return lastStatus;
        }

        public void verify() {
            ObjectNode document = JsonNodeFactory.instance.objectNode();
            document.put("scenario", id);
            document.put("title", title);
            document.set("steps", steps);
            A2Golden.verify(id, document, requestValues, placeholders);
        }

        private Scenario send(String label, Group group, String method, String path, String login,
                              Map<String, String> headers, Object body) {
            try {
                MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path)
                    .contentType(MediaType.APPLICATION_JSON);
                if (login != null) {
                    builder.header("Authorization", "Bearer " + A2Tokens.token(jwtKey, login));
                }
                headers.forEach(builder::header);
                JsonNode requestBody = null;
                if (body instanceof JsonNode node) {
                    requestBody = node;
                    builder.content(json.writeValueAsString(node));
                    collect(node);
                } else if (body instanceof String raw) {
                    builder.content(raw);
                }
                MockHttpServletResponse response = mvc.perform(builder).andReturn().getResponse();

                ObjectNode request = JsonNodeFactory.instance.objectNode();
                request.put("method", method);
                request.put("path", path);
                request.put("as", login);
                if (group != null) {
                    request.put("phone", group.name());
                }
                if (!headers.isEmpty()) {
                    request.set("headers", json.valueToTree(headers));
                }
                if (requestBody != null) {
                    request.set("body", requestBody.deepCopy());
                } else if (body instanceof String raw) {
                    request.put("rawBody", raw);
                }

                lastStatus = response.getStatus();
                String content = response.getContentAsString(StandardCharsets.UTF_8);
                lastBody = parse(content);
                ObjectNode answer = JsonNodeFactory.instance.objectNode();
                answer.put("status", lastStatus);
                ObjectNode answerHeaders = JsonNodeFactory.instance.objectNode();
                for (String name : response.getHeaderNames()) {
                    if (name.equalsIgnoreCase("Content-Type") || name.equalsIgnoreCase("WWW-Authenticate")
                        || name.startsWith("X-dataRunApiApp")) {
                        answerHeaders.put(name, response.getHeader(name));
                    }
                }
                answer.set("headers", answerHeaders);
                answer.set("body", maskResponseTimes(lastBody.deepCopy()));

                if (group != null) {
                    List<String> violations = A2PhoneContract.violations(group, requestBody, lastStatus, lastBody);
                    assertThat(violations).as("phone contract %s in %s", group, label).isEmpty();
                }

                ObjectNode step = JsonNodeFactory.instance.objectNode();
                step.put("step", steps.size() + 1);
                step.put("label", label);
                step.set("request", request);
                step.set("response", answer);
                step.set("db", snapshot());
                steps.add(step);
                return this;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private void collect(JsonNode node) {
            if (node.isTextual()) {
                requestValues.add(node.asText());
            }
            node.forEach(this::collect);
        }

        private JsonNode parse(String content) {
            if (content == null || content.isEmpty()) {
                return NullNode.getInstance();
            }
            try {
                return json.readTree(content);
            } catch (Exception e) {
                return TextNode.valueOf(content);
            }
        }

        private JsonNode maskResponseTimes(JsonNode node) {
            if (node.isObject()) {
                ObjectNode object = (ObjectNode) node;
                List<String> names = new java.util.ArrayList<>();
                object.fieldNames().forEachRemaining(names::add);
                for (String name : names) {
                    JsonNode value = object.get(name);
                    if (sortedArrays.containsKey(name) && value.isArray()) {
                        String field = sortedArrays.get(name);
                        List<JsonNode> elements = new java.util.ArrayList<>();
                        value.forEach(elements::add);
                        elements.sort(java.util.Comparator.comparing(element -> element.path(field).asText()));
                        ArrayNode sorted = JsonNodeFactory.instance.arrayNode();
                        elements.forEach(element -> sorted.add(maskResponseTimes(element)));
                        object.set(name, sorted);
                    } else if (summarized.contains(name) && value.isArray()) {
                        object.put(name, "<" + value.size() + " items>");
                    } else if ("detail".equals(name) && value.isTextual() && value.asText().contains("ERROR: ")) {
                        // a database error quotes the failing row: ids, serials and the app clock; keep its cause
                        String detail = value.asText();
                        int start = detail.indexOf("ERROR: ");
                        int end = detail.indexOf('\n', start);
                        object.put(name, "<database error> " + detail.substring(start, end < 0 ? detail.length() : end));
                    } else if (List.of("createdDate", "lastModifiedDate", "lastSeen").contains(name) && !value.isNull()) {
                        object.put(name, "<set>");
                    } else if ("serialNumber".equals(name) && value.isNumber()) {
                        object.put(name, serials.computeIfAbsent(value.asText(),
                            serial -> "<serial:" + (serials.size() + 1) + ">"));
                    } else {
                        maskResponseTimes(value);
                    }
                }
            } else if (node.isArray()) {
                node.forEach(this::maskResponseTimes);
            }
            return node;
        }

        ObjectNode snapshot() {
            MapSqlParameterSource scope = scope();
            ObjectNode db = JsonNodeFactory.instance.objectNode();
            db.set("data_submission", rows("""
                SELECT id, uid, serial_number, template_uid, template_version_uid, template_version_no, assignment_uid,
                       team_uid, team_code, org_unit_uid, org_unit_code, org_unit_name, activity_uid, status, deleted,
                       deleted_at, form_data::text AS form_data, extract(epoch FROM start_entry_time) AS start_entry_time,
                       extract(epoch FROM finished_entry_time) AS finished_entry_time, lock_version, created_by,
                       last_modified_by, created_date, last_modified_date
                FROM data_submission WHERE uid LIKE 'a2%' OR assignment_uid IN (:assignments)
                ORDER BY serial_number""", scope));
            db.set("outbox", rows("""
                SELECT submission_id, submission_serial_number, submission_uid, topic, event_type,
                       payload::text AS payload, status, attempt, last_error, created_at
                FROM outbox WHERE submission_uid LIKE 'a2%'
                   OR submission_id IN (SELECT id FROM data_submission WHERE assignment_uid IN (:assignments))
                ORDER BY outbox_id""", scope));
            db.set("held_submission", rows("""
                SELECT uid, reason, state, activity_uid, org_unit_uid, org_unit_path, form_uid, form_version_uid,
                       team_uid, assignment_uid, submitted_by, extract(epoch FROM started_at) AS started_at, received_at,
                       go_live_rule_uid, extract(epoch FROM live_from) AS live_from, payload::text AS payload,
                       decided_by, decided_at, note, last_release_error, last_modified_date AS held_last_modified_date
                FROM held_submission WHERE uid LIKE 'a2%' OR assignment_uid IN (:assignments)
                ORDER BY uid""", scope));
            db.set("go_live_rule", rows("""
                SELECT uid, org_unit_uid, form_uid, extract(epoch FROM live_from) AS live_from
                FROM go_live_rule WHERE activity_uid = :activity
                ORDER BY org_unit_uid NULLS FIRST, form_uid NULLS FIRST""", scope));
            db.set("reference_entry", rows("""
                SELECT r.uid, r.display_name, ou.uid AS org_unit_uid, a.uid AS first_registered_activity_uid,
                       r.created_by
                FROM reference_entry r
                JOIN org_unit ou ON ou.id = r.org_unit_id
                LEFT JOIN activity a ON a.id = r.first_registered_activity_id
                WHERE r.created_by <> 'a2-seed' AND ou.uid IN (:orgUnits)
                ORDER BY r.uid""", scope));
            return db;
        }

        private ArrayNode rows(String sql, MapSqlParameterSource params) {
            ArrayNode rows = JsonNodeFactory.instance.arrayNode();
            for (Map<String, Object> row : jdbc.queryForList(sql, params)) {
                ObjectNode node = JsonNodeFactory.instance.objectNode();
                row.forEach((column, value) -> node.set(column, cell(column, value)));
                rows.add(node);
            }
            return rows;
        }

        private JsonNode cell(String column, Object value) {
            if (value == null) {
                return NullNode.getInstance();
            }
            if (SERVER_TIMES.contains(column)) {
                return TextNode.valueOf("<set>");
            }
            if (EPOCH_TIMES.contains(column)) {
                BigDecimal seconds = new BigDecimal(value.toString());
                Instant instant = Instant.ofEpochSecond(seconds.longValue(),
                    seconds.remainder(BigDecimal.ONE).movePointRight(9).longValue());
                return TextNode.valueOf(DateTimeFormatter.ISO_INSTANT.format(instant));
            }
            if (SERIALS.contains(column)) {
                return TextNode.valueOf(serials.computeIfAbsent(value.toString(),
                    serial -> "<serial:" + (serials.size() + 1) + ">"));
            }
            if (JSON_COLUMNS.contains(column)) {
                return parse(value.toString());
            }
            return json.valueToTree(value);
        }
    }
}
