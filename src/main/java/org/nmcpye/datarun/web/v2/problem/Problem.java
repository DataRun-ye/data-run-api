package org.nmcpye.datarun.web.v2.problem;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The body of every v2 error response, {@code application/problem+json} (RFC 9457):
 * <ul>
 *   <li>{@code type}, {@code title} and {@code status} come from the {@link ProblemCode}; {@code code} is the code
 *       itself, the short form clients switch on;</li>
 *   <li>{@code detail} explains this occurrence in words, never with a submitted value, a login or an exception
 *       message;</li>
 *   <li>{@code instance} is {@code urn:uuid:} and the request id, also sent as {@code X-Request-ID}, so a report
 *       quoting it finds the request's log lines;</li>
 *   <li>{@code errors} lists findings with their locations ({@link ProblemError});</li>
 *   <li>extension members, such as {@code currentVersion} on a 412, sit beside the standard members. Clients
 *       ignore members they do not know, as RFC 9457 requires.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"type", "title", "status", "detail", "instance", "code", "errors"})
public final class Problem {

    /** Members an extension may not take: the standard ones and this API's own. */
    static final Set<String> RESERVED = Set.of("type", "title", "status", "detail", "instance", "code", "errors");

    private final ProblemCode code;
    private final int status;
    private final String detail;
    private final URI instance;
    private final List<ProblemError> errors;
    private final Map<String, Object> extensions;

    public Problem(ProblemCode code, int status, String detail, URI instance, List<ProblemError> errors,
                   Map<String, Object> extensions) {
        this.code = code;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        Map<String, Object> copy = new LinkedHashMap<>();
        if (extensions != null) {
            extensions.forEach((name, value) -> {
                if (RESERVED.contains(name)) {
                    throw new IllegalArgumentException("A problem extension may not be named " + name);
                }
                copy.put(name, value);
            });
        }
        this.extensions = Collections.unmodifiableMap(copy);
    }

    public URI getType() {
        return code.type();
    }

    public String getTitle() {
        return code.title();
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public URI getInstance() {
        return instance;
    }

    public String getCode() {
        return code.code();
    }

    public List<ProblemError> getErrors() {
        return errors;
    }

    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    /** The registry entry, for code that inspects a problem; not serialized apart from {@link #getCode()}. */
    public ProblemCode problemCode() {
        return code;
    }
}
