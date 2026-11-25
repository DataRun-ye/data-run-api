package org.nmcpye.datarun.web.v2.problem;

import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How v2 code refuses a request: throw this with a {@link ProblemCode}, and the v2 problem resolver answers with
 * that problem. The status is the code's; {@link #detail}, {@link #error findings}, {@link #extension extension
 * members} and {@link #header headers} add to it.
 * <p>
 * Its message is the code alone and it has no stack trace: a refusal is an expected outcome, and nothing here may
 * carry a submitted value into a log line.
 */
public class ApiProblemException extends RuntimeException {

    private final ProblemCode code;
    private String detail;
    private final List<ProblemError> errors = new ArrayList<>();
    private final Map<String, Object> extensions = new LinkedHashMap<>();
    private final HttpHeaders headers = new HttpHeaders();

    public ApiProblemException(ProblemCode code) {
        super(code.code(), null, false, false);
        this.code = code;
    }

    public static ApiProblemException of(ProblemCode code) {
        return new ApiProblemException(code);
    }

    /** The occurrence explained in words, without any submitted value. */
    public ApiProblemException detail(String detail) {
        this.detail = detail;
        return this;
    }

    public ApiProblemException error(ProblemError error) {
        errors.add(error);
        return this;
    }

    public ApiProblemException extension(String name, Object value) {
        if (Problem.RESERVED.contains(name)) {
            throw new IllegalArgumentException("A problem extension may not be named " + name);
        }
        extensions.put(name, value);
        return this;
    }

    public ApiProblemException header(String name, String value) {
        headers.add(name, value);
        return this;
    }

    public ProblemCode code() {
        return code;
    }

    public String detail() {
        return detail;
    }

    public List<ProblemError> errors() {
        return List.copyOf(errors);
    }

    public Map<String, Object> extensions() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(extensions));
    }

    public HttpHeaders headers() {
        return HttpHeaders.readOnlyHttpHeaders(headers);
    }
}
