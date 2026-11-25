package org.nmcpye.datarun.web.v2.problem;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.activation.OperationNotEnabledException;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each kind of failure a v2 request can meet, answered as its problem, with one log line that carries no message,
 * body or value; and every request outside v2 left to the v1 error handling.
 */
class V2ProblemResolverTest {

    private static final String REQUEST_ID = "0b6c1c3e-5d0e-4f27-9a37-6a0f2f7f3b11";
    private static final String SECRET = "s3cr3t-value-0779";

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final V2ProblemResolver resolver = new V2ProblemResolver(new ProblemWriter(json));

    private final Logger logger = (Logger) LoggerFactory.getLogger(V2ProblemResolver.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        lines.start();
        logger.addAppender(lines);
        MDC.put(RequestIdFilter.MDC_REQUEST_ID_KEY, REQUEST_ID);
    }

    @AfterEach
    void release() {
        logger.detachAppender(lines);
        MDC.remove(RequestIdFilter.MDC_REQUEST_ID_KEY);
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v2/things/{id}");
        return request;
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        assertThat(response.getContentType()).startsWith("application/problem+json");
        return json.readTree(response.getContentAsByteArray());
    }

    @Test
    void requestsOutsideV2AreLeftToTheV1Handling() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        for (String path : List.of("/api/v1/things", "/api/custom/x", "/api/v2x", "/management/health")) {
            assertThat(resolver.resolveException(request("GET", path), response, null, new RuntimeException()))
                .as(path).isNull();
        }
        assertThat(response.getContentAsByteArray()).isEmpty();
        assertThat(lines.list).isEmpty();
    }

    @Test
    void aRefusalIsItsProblemWithDetailErrorsExtensionsAndHeaders() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiProblemException refusal = ApiProblemException.of(ProblemCode.PRECONDITION_FAILED)
            .detail("The resource is at version 3.")
            .error(ProblemError.atHeader("If-Match", "stale", "Read the resource again."))
            .extension("currentVersion", 3)
            .header(HttpHeaders.ETAG, "\"3\"");

        ModelAndView handled = resolver.resolveException(request("POST", "/api/v2/things/7"), response, null, refusal);

        assertThat(handled).isNotNull();
        assertThat(response.getStatus()).isEqualTo(412);
        assertThat(response.getHeader(HttpHeaders.ETAG)).isEqualTo("\"3\"");
        JsonNode body = body(response);
        assertThat(body.get("type").asText()).isEqualTo("tag:nmcpye.org,2026:datarun/problem/precondition-failed");
        assertThat(body.get("title").asText()).isEqualTo(ProblemCode.PRECONDITION_FAILED.title());
        assertThat(body.get("status").asInt()).isEqualTo(412);
        assertThat(body.get("detail").asText()).isEqualTo("The resource is at version 3.");
        assertThat(body.get("instance").asText()).isEqualTo("urn:uuid:" + REQUEST_ID);
        assertThat(body.get("code").asText()).isEqualTo("precondition-failed");
        assertThat(body.get("errors").get(0).get("header").asText()).isEqualTo("If-Match");
        assertThat(body.get("errors").get(0).has("pointer")).isFalse();
        assertThat(body.get("currentVersion").asInt()).isEqualTo(3);
        assertThat(lines.list).singleElement().satisfies(line -> {
            assertThat(line.getLevel()).isEqualTo(Level.INFO);
            assertThat(line.getFormattedMessage()).isEqualTo("v2 request POST /api/v2/things/{id} answered 412 "
                + "(precondition-failed): ApiProblemException, request " + REQUEST_ID);
        });
    }

    @Test
    void aBodyThatIsNotJsonIsMalformedAndItsTextIsNeverRepeated() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpMessageNotReadableException unreadable = new HttpMessageNotReadableException("JSON parse error: " + SECRET,
            new JsonParseException(null, "Unexpected character " + SECRET), input());

        resolver.resolveException(request("POST", "/api/v2/things/7"), response, null, unreadable);

        assertThat(response.getStatus()).isEqualTo(400);
        JsonNode body = body(response);
        assertThat(body.get("code").asText()).isEqualTo("bad-request");
        assertThat(body.get("detail").asText()).isEqualTo("The request body is not valid JSON.");
        assertThat(response.getContentAsString()).doesNotContain(SECRET);
        assertNoLineLeaks();
    }

    @Test
    void aValueOfTheWrongTypeIsLocatedByAPointerWithoutRepeatingTheValue() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        InvalidFormatException format = InvalidFormatException.from(null, "Cannot parse " + SECRET, SECRET, Integer.class);
        format.prependPath(new JsonMappingException.Reference(null, "value"));
        format.prependPath(new JsonMappingException.Reference(null, 1));
        format.prependPath(new JsonMappingException.Reference(null, "operations"));
        HttpMessageNotReadableException unreadable =
            new HttpMessageNotReadableException("JSON parse error: " + SECRET, format, input());

        resolver.resolveException(request("POST", "/api/v2/things/7"), response, null, unreadable);

        JsonNode error = body(response).get("errors").get(0);
        assertThat(error.get("pointer").asText()).isEqualTo("#/operations/1/value");
        assertThat(error.get("code").asText()).isEqualTo("invalid-type");
        assertThat(error.get("detail").asText()).isEqualTo("Expected a number.");
        assertThat(response.getContentAsString()).doesNotContain(SECRET).doesNotContain("java.");
        assertNoLineLeaks();
    }

    @Test
    void aBodyFailingBeanValidationIs422WithAPointerPerFinding() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "request");
        result.addError(new FieldError("request", "operations[1].value", SECRET, false,
            new String[] {"NotBlank"}, null, "must not be blank"));
        MethodArgumentNotValidException invalid = new MethodArgumentNotValidException(
            new MethodParameter(V2ProblemResolverTest.class.getDeclaredMethod("input"), -1), result);

        resolver.resolveException(request("POST", "/api/v2/things/7"), response, null, invalid);

        assertThat(response.getStatus()).isEqualTo(422);
        JsonNode body = body(response);
        assertThat(body.get("code").asText()).isEqualTo("validation-failed");
        assertThat(body.get("errors").get(0).get("pointer").asText()).isEqualTo("#/operations/1/value");
        assertThat(body.get("errors").get(0).get("code").asText()).isEqualTo("not-blank");
        assertThat(response.getContentAsString()).doesNotContain(SECRET);
    }

    @Test
    void routingFailuresBeforeAnyHandlerKeepTheirStatusAndHeaders() throws Exception {
        MockHttpServletResponse notAllowed = new MockHttpServletResponse();
        resolver.resolveException(request("DELETE", "/api/v2/things/7"), notAllowed, null,
            new HttpRequestMethodNotSupportedException("DELETE", Set.of("GET", "POST")));
        assertThat(notAllowed.getStatus()).isEqualTo(405);
        assertThat(notAllowed.getHeader(HttpHeaders.ALLOW)).contains("GET").contains("POST");
        assertThat(body(notAllowed).get("code").asText()).isEqualTo("method-not-allowed");

        MockHttpServletResponse missing = new MockHttpServletResponse();
        resolver.resolveException(request("GET", "/api/v2/nothing-here"), missing, null,
            new NoResourceFoundException(HttpMethod.GET, "api/v2/nothing-here"));
        assertThat(missing.getStatus()).isEqualTo(404);
        assertThat(body(missing).get("code").asText()).isEqualTo("not-found");
        assertThat(missing.getContentAsString()).doesNotContain("nothing-here");

        MockHttpServletResponse unsupported = new MockHttpServletResponse();
        resolver.resolveException(request("POST", "/api/v2/things/7"), unsupported, null,
            new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)));
        assertThat(unsupported.getStatus()).isEqualTo(415);
        assertThat(unsupported.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
        assertThat(body(unsupported).get("code").asText()).isEqualTo("unsupported-media-type");
    }

    @Test
    void aLockWaitThatTimesOutIsATransientRefusal() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.resolveException(request("POST", "/api/v2/things/7"), response, null,
            new CannotAcquireLockException("could not obtain lock " + SECRET));
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
        assertThat(body(response).get("code").asText()).isEqualTo("resource-busy");
        assertNoLineLeaks();
    }

    @Test
    void anOperationSwitchedOffIs422NamingItsSwitch() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.resolveException(request("POST", "/api/v2/things/7"), response, null,
            new OperationNotEnabledException("some-feature"));
        assertThat(response.getStatus()).isEqualTo(422);
        JsonNode body = body(response);
        assertThat(body.get("code").asText()).isEqualTo("operation-not-enabled");
        assertThat(body.get("detail").asText()).contains("some-feature");
    }

    /** Spring's default JDBC translation leaves PostgreSQL's lock timeout uncategorized; its state decides. */
    @Test
    void aLockTimeoutKnownOnlyByItsSqlStateIsTransientToo() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.resolveException(request("POST", "/api/v2/things/7"), response, null,
            new UncategorizedSQLException("update", "UPDATE t SET v = 1",
                new SQLException("canceling statement due to lock timeout", "55P03")));
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(body(response).get("code").asText()).isEqualTo("resource-busy");
        assertThat(response.getContentAsString()).doesNotContain("UPDATE");
    }

    @Test
    void anUnexpectedFailureIsAServerFaultLoggedAtErrorWithItsShapeOnly() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolver.resolveException(request("GET", "/api/v2/things/7"), response, null,
            new IllegalStateException("failed on " + SECRET, new IllegalArgumentException(SECRET)));

        assertThat(response.getStatus()).isEqualTo(500);
        JsonNode body = body(response);
        assertThat(body.get("code").asText()).isEqualTo("internal-error");
        assertThat(response.getContentAsString()).doesNotContain(SECRET);
        assertThat(lines.list).singleElement().satisfies(line -> {
            assertThat(line.getLevel()).isEqualTo(Level.ERROR);
            assertThat(line.getMessage()).isEqualTo(V2ProblemResolver.FAULT_LINE);
            assertThat(line.getFormattedMessage())
                .startsWith("v2 request GET /api/v2/things/{id} failed with 500 (internal-error): "
                    + "IllegalStateException < IllegalArgumentException, at V2ProblemResolverTest.");
        });
        assertNoLineLeaks();
    }

    /** FailureShape's fallback: a root cause thrown in library code shows where the application met it. */
    @Test
    void aFaultWhoseRootCauseHasNoApplicationFrameShowsTheOuterFrames() {
        IllegalArgumentException root = new IllegalArgumentException(SECRET);
        root.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("java.util.Objects", "requireNonNull", "Objects.java", 233)});
        resolver.resolveException(request("GET", "/api/v2/things/7"), new MockHttpServletResponse(), null,
            new IllegalStateException(SECRET, root));

        assertThat(lines.list).singleElement().satisfies(line -> assertThat(line.getFormattedMessage())
            .contains("IllegalStateException < IllegalArgumentException, at V2ProblemResolverTest."));
        assertNoLineLeaks();
    }

    @Test
    void withoutARequestIdYetTheClientsUuidOrANewOneIsUsedAndReturned() throws Exception {
        MDC.remove(RequestIdFilter.MDC_REQUEST_ID_KEY);
        MockHttpServletRequest sent = request("GET", "/api/v2/x");
        sent.addHeader(RequestIdFilter.REQUEST_ID_HEADER, REQUEST_ID);
        MockHttpServletResponse withClientId = new MockHttpServletResponse();
        resolver.resolveException(sent, withClientId, null, ApiProblemException.of(ProblemCode.NOT_FOUND));
        assertThat(body(withClientId).get("instance").asText()).isEqualTo("urn:uuid:" + REQUEST_ID);
        assertThat(withClientId.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(REQUEST_ID);

        MockHttpServletRequest login = request("GET", "/api/v2/x");
        login.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "someone@example.org");
        MockHttpServletResponse drawn = new MockHttpServletResponse();
        resolver.resolveException(login, drawn, null, ApiProblemException.of(ProblemCode.NOT_FOUND));
        String drawnId = drawn.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(drawnId).matches("[0-9a-f-]{36}");
        assertThat(body(drawn).get("instance").asText()).isEqualTo("urn:uuid:" + drawnId);
    }

    @Test
    void aCommittedResponseIsLeftAloneButTheFailureIsStillAnsweredAndLogged() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);
        assertThat(resolver.resolveException(request("GET", "/api/v2/x"), response, null, new RuntimeException()))
            .isNotNull();
        assertThat(response.getContentAsByteArray()).isEmpty();
        assertThat(lines.list).hasSize(1);
    }

    @Test
    void fieldPathsBecomeJsonPointersAndConstraintCodesKebabCase() {
        assertThat(V2ProblemResolver.fieldPointer("operations[1].value")).isEqualTo("#/operations/1/value");
        assertThat(V2ProblemResolver.fieldPointer("a.b[0][2].c")).isEqualTo("#/a/b/0/2/c");
        assertThat(V2ProblemResolver.fieldPointer("reason")).isEqualTo("#/reason");
        assertThat(ProblemError.pointer(List.of("a/b", "c~d"))).isEqualTo("#/a~1b/c~0d");
        assertThat(ProblemError.pointer(List.of("a b", "\u0645", "x%y"))).isEqualTo("#/a%20b/%D9%85/x%25y");
        assertThat(ProblemError.pointer(List.of())).isEqualTo("#");
        assertThat(V2ProblemResolver.kebab("NotBlank")).isEqualTo("not-blank");
        assertThat(V2ProblemResolver.kebab("Size")).isEqualTo("size");
        assertThat(V2ProblemResolver.kebab(null)).isEqualTo("invalid");
    }

    private void assertNoLineLeaks() {
        assertThat(lines.list).isNotEmpty();
        for (ILoggingEvent line : lines.list) {
            assertThat(line.getThrowableProxy()).as("no exception, whose message can quote values").isNull();
            assertThat(line.getFormattedMessage()).doesNotContain(SECRET);
        }
    }

    private static HttpInputMessage input() {
        return new MockHttpInputMessage(new byte[0]);
    }
}
