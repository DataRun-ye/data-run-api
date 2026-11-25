package org.nmcpye.datarun.web.v2.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Writes a v2 problem response: the status, the given headers, {@code application/problem+json} and the
 * {@link Problem} body. The MVC resolver and the security chain's handlers both write through it, so an error
 * answered before or after authentication has one shape.
 */
@Component
public class ProblemWriter {

    public static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private static final Pattern UUID_TEXT =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final ObjectMapper objectMapper;

    public ProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** The problem for this request, with the code's status and its {@code instance} set from the request id. */
    public Problem problem(HttpServletRequest request, HttpServletResponse response, ProblemCode code, String detail,
                           List<ProblemError> errors, Map<String, Object> extensions) {
        return problem(request, response, code, code.status(), detail, errors, extensions);
    }

    /** As {@link #problem(HttpServletRequest, HttpServletResponse, ProblemCode, String, List, Map)}, at a status. */
    public Problem problem(HttpServletRequest request, HttpServletResponse response, ProblemCode code, int status,
                           String detail, List<ProblemError> errors, Map<String, Object> extensions) {
        return new Problem(code, status, detail, instance(request, response), errors, extensions);
    }

    public void write(HttpServletResponse response, Problem problem, HttpHeaders headers) throws IOException {
        response.setStatus(problem.getStatus());
        if (headers != null) {
            headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        }
        response.setContentType(PROBLEM_JSON.toString());
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    /**
     * {@code urn:uuid:} and the request's id. {@link RequestIdFilter} sets that id for every request that reaches
     * MVC; the security chain answers before that filter runs, so there the client's {@code X-Request-ID} is taken
     * when it is a UUID, as that filter would, or a new id is drawn, and the response carries it.
     */
    static URI instance(HttpServletRequest request, HttpServletResponse response) {
        String id = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        if (id == null) {
            String sent = request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
            id = sent != null && UUID_TEXT.matcher(sent).matches() ? sent : UUID.randomUUID().toString();
            if (!response.containsHeader(RequestIdFilter.REQUEST_ID_HEADER)) {
                response.setHeader(RequestIdFilter.REQUEST_ID_HEADER, id);
            }
        }
        // As the request id reads in the log lines, so a search for it finds them.
        return URI.create("urn:uuid:" + id);
    }
}
