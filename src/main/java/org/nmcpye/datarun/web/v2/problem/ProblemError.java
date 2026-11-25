package org.nmcpye.datarun.web.v2.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One finding in a problem's {@code errors} list: what is wrong ({@code code}) and where. At most one locator is
 * set: {@code pointer}, a JSON Pointer into the request body in RFC 9457's URI-fragment form ({@code #/reason});
 * {@code parameter}, a query or path parameter; or {@code header}, a request header. {@code field} names a form
 * field by id, for findings about a value. {@code detail} explains it in words. None of them ever repeats a
 * submitted value or a caller's reason.
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
     * A JSON Pointer (RFC 6901) from path segments, in its URI-fragment form as RFC 9457 writes it: each segment
     * escaped ({@code ~} as {@code ~0}, {@code /} as {@code ~1}), then percent-encoded where a fragment needs it.
     * {@code ["operations", "1", "value"]} becomes {@code #/operations/1/value}; no segment is {@code #}, the
     * whole document.
     */
    public static String pointer(Iterable<String> segments) {
        StringBuilder pointer = new StringBuilder("#");
        for (String segment : segments) {
            pointer.append('/').append(fragment(segment.replace("~", "~0").replace("/", "~1")));
        }
        return pointer.toString();
    }

    /** Percent-encodes, as UTF-8, every character a URI fragment may not hold literally (RFC 3986 section 3.5). */
    private static String fragment(String text) {
        StringBuilder out = new StringBuilder();
        for (byte b : text.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || "-._~!$&'()*+,;=:@/?".indexOf(c) >= 0) {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return out.toString();
    }
}
