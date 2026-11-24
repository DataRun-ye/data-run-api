package org.nmcpye.datarun.web.rest.errors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * What {@code error_event} keeps of a request error: its code, its exception type, a stack trace of types and frames,
 * and identifiers; never an exception's message, which quotes submitted values, request bodies or database rows
 * (production's records held all three until 6.5.5). The test profiles do not enable {@code @Async}, so a record is
 * written before the answer returns.
 */
@AutoConfigureMockMvc
@IntegrationTest
class ErrorEventRecordIT {

    private static final String USER_ID = "01JUSER0000000000000000009";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void aDatabaseFailureIsRecordedByItsCodeTypesSqlStateAndConstraint() throws Exception {
        Map<String, Object> row = record(get("/api/exception-translator-test/database-failure"));

        assertThat(row).containsEntry("status", 500)
            .containsEntry("message", "error.http.500")
            .containsEntry("exception", "org.springframework.dao.DataIntegrityViolationException");
        assertThat((String) row.get("stacktrace"))
            .startsWith("org.springframework.dao.DataIntegrityViolationException\n\tat ");
        JsonNode context = json.readTree((String) row.get("context_json"));
        assertThat(context.path("causes").asText()).isEqualTo("DataIntegrityViolationException < PSQLException");
        assertThat(context.path("sqlState").asText()).isEqualTo("23505");
        assertThat(context.path("constraint").asText()).isEqualTo("uk_test_name");
        assertThat(context.path("userId").asText()).isEqualTo(USER_ID);
        assertNoValues(row, "secret-sql-7", "secret-detail-7");
    }

    @Test
    void aRejectedFieldIsRecordedWithoutTheRejectedValue() throws Exception {
        Map<String, Object> row = record(post("/api/exception-translator-test/method-argument-size")
            .contentType(MediaType.APPLICATION_JSON).content("{\"code\": \"secret-rejected-7\"}"));

        assertThat(row).containsEntry("status", 400)
            .containsEntry("message", ErrorConstants.ERR_VALIDATION)
            .containsEntry("exception", "org.springframework.web.bind.MethodArgumentNotValidException");
        assertThat(json.readTree((String) row.get("context_json")).path("validationErrors").asInt()).isEqualTo(1);
        assertNoValues(row, "secret-rejected-7");
    }

    @Test
    void anUnreadableBodyIsRecordedWithoutItsFragments() throws Exception {
        Map<String, Object> row = record(post("/api/exception-translator-test/method-argument")
            .contentType(MediaType.APPLICATION_JSON).content("{\"test\": secrettoken7}"));

        assertThat(row).containsEntry("status", 400)
            .containsEntry("message", "error.http.400")
            .containsEntry("exception", "org.springframework.http.converter.HttpMessageNotReadableException");
        assertThat(json.readTree((String) row.get("context_json")).path("causes").asText())
            .isEqualTo("HttpMessageNotReadableException < JsonParseException");
        assertNoValues(row, "secrettoken7");
    }

    /** Sends the request as a user with a durable id and returns the record whose request id the answer carries. */
    private Map<String, Object> record(MockHttpServletRequestBuilder request) throws Exception {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getUsername()).thenReturn("field-login-9");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(user).getAuthorities();
        MvcResult result = mockMvc.perform(request.with(authentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())))).andReturn();
        String requestId = result.getResponse().getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank();

        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT status, message, exception, stacktrace, context_json, path FROM error_event "
                + "WHERE context_json LIKE ?", "%\"requestId\":\"" + requestId + "\"%");
        assertThat(rows).as("the record of request %s", requestId).hasSize(1);
        return rows.get(0);
    }

    private static void assertNoValues(Map<String, Object> row, String... values) {
        for (Map.Entry<String, Object> column : row.entrySet()) {
            for (String value : values) {
                assertThat(String.valueOf(column.getValue())).as("error_event.%s", column.getKey())
                    .doesNotContain(value);
            }
        }
    }
}
