package org.nmcpye.datarun.web.v2.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One finding in a problem's {@code errors} list: what is wrong ({@code code}) and where. At most one locator is
 * set: {@code pointer}, an RFC 6901 JSON Pointer into the request body; {@code parameter}, a query or path
 * parameter; or {@code header}, a request header. {@code field} names a form field by id, for findings about a
 * value. {@code detail} explains it in words. None of them ever repeats a submitted value.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "detail", "pointer", "parameter", "header", "field"})
public record ProblemError(String code, String detail, String pointer, String parameter, String header,
                           String field) {

    public static ProblemError atPointer(String pointer, String code, String detail) {
        return new ProblemError(code, detail, pointer, null, null, null);
    }

    public static ProblemError atParameter(String parameter, String code, String detail) {
        return new ProblemError(code, detail, null, parameter, null, null);
    }

    public static ProblemError atHeader(String header, String code, String detail) {
        return new ProblemError(code, detail, null, null, header, null);
    }

    /**
     * An RFC 6901 pointer from path segments, each escaped ({@code ~} as {@code ~0}, {@code /} as {@code ~1}):
     * {@code ["operations", "1", "value"]} becomes {@code /operations/1/value}; no segment is the whole document.
     */
    public static String pointer(Iterable<String> segments) {
        StringBuilder pointer = new StringBuilder();
        for (String segment : segments) {
            pointer.append('/').append(segment.replace("~", "~0").replace("/", "~1"));
        }
        return pointer.toString();
    }
}
