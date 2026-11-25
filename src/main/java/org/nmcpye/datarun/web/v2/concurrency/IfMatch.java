package org.nmcpye.datarun.web.v2.concurrency;

import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A request's {@code If-Match} precondition (RFC 9110 section 13.1.1): {@code *} or a list of entity tags.
 * <p>
 * v2 commands take it as their lost-update guard: a command must name the version it was decided on, so a missing
 * header and {@code *} are both answered 428 ({@link #required}), a header that is not a list of entity tags 400,
 * and a list none of whose tags strongly matches the resource's current tag 412 ({@link #check}).
 *
 * @param any  whether the header was {@code *}
 * @param tags the listed tags, empty for {@code *}
 */
public record IfMatch(boolean any, List<EntityTag> tags) {

    public static final String HEADER = HttpHeaders.IF_MATCH;

    public IfMatch {
        tags = List.copyOf(tags);
        if (any != tags.isEmpty()) {
            throw new IllegalArgumentException("If-Match is either * or a non-empty list of entity tags");
        }
    }

    /**
     * Parses the request's {@code If-Match} field lines, joined as one list; {@code null} when there are none.
     *
     * @throws ApiProblemException 400 {@code invalid-if-match} when the value is not {@code *} or a list of entity
     *                             tags
     */
    public static IfMatch parse(List<String> fieldLines) {
        if (fieldLines == null || fieldLines.isEmpty()) {
            return null;
        }
        String value = String.join(",", fieldLines).trim();
        if (value.equals("*")) {
            return new IfMatch(true, List.of());
        }
        List<EntityTag> tags = new ArrayList<>();
        int i = 0;
        int n = value.length();
        while (i < n) {
            char c = value.charAt(i);
            if (c == ' ' || c == '\t' || c == ',') {
                i++;
                continue;
            }
            boolean weak = false;
            if (c == 'W' && i + 1 < n && value.charAt(i + 1) == '/') {
                weak = true;
                i += 2;
            }
            if (i >= n || value.charAt(i) != '"') {
                throw invalid();
            }
            int end = value.indexOf('"', i + 1);
            if (end < 0) {
                throw invalid();
            }
            String tag = value.substring(i + 1, end);
            for (int k = 0; k < tag.length(); k++) {
                if (!EntityTag.isTagChar(tag.charAt(k))) {
                    throw invalid();
                }
            }
            tags.add(new EntityTag(tag, weak));
            i = end + 1;
            // The next tag must be separated by a comma.
            while (i < n && (value.charAt(i) == ' ' || value.charAt(i) == '\t')) {
                i++;
            }
            if (i < n && value.charAt(i) != ',') {
                throw invalid();
            }
        }
        if (tags.isEmpty()) {
            throw invalid();
        }
        return new IfMatch(false, tags);
    }

    /**
     * The precondition a command needs: a list of entity tags.
     *
     * @throws ApiProblemException 428 {@code precondition-required} when there is no {@code If-Match} or it is
     *                             {@code *}
     */
    public static IfMatch required(IfMatch ifMatch) {
        if (ifMatch == null || ifMatch.any()) {
            throw ApiProblemException.of(ProblemCode.PRECONDITION_REQUIRED)
                .detail(ifMatch == null
                    ? "Send If-Match with the entity tag (ETag) of the version this request was decided on."
                    : "If-Match: * does not name a version; send the resource's entity tag (ETag).")
                .error(ProblemError.atHeader(HEADER, ifMatch == null ? "missing" : "wildcard", null));
        }
        return ifMatch;
    }

    /**
     * Checks the precondition against the resource's current version, by strong comparison.
     *
     * @param currentVersion the version of the resource as the request's transaction holds it (locked)
     * @param versionHistory a link to the resource's versions, or {@code null}; sent as
     *                       {@code Link: <...>; rel="version-history"} on a 412
     * @throws ApiProblemException 412 {@code precondition-failed} with {@code currentVersion}, the current
     *                             {@code ETag} and the link
     */
    public void check(long currentVersion, String versionHistory) {
        EntityTag current = EntityTag.ofVersion(currentVersion);
        if (tags.stream().anyMatch(tag -> tag.strongMatches(current))) {
            return;
        }
        ApiProblemException stale = ApiProblemException.of(ProblemCode.PRECONDITION_FAILED)
            .detail("The resource is at version " + currentVersion + "; read it again before deciding.")
            .extension("currentVersion", currentVersion)
            .header(HttpHeaders.ETAG, current.toString());
        if (versionHistory != null) {
            stale.header(HttpHeaders.LINK, "<" + versionHistory + ">; rel=\"version-history\"");
        }
        throw stale;
    }

    /** The precondition in a canonical form, for the request fingerprint: {@code "3", W/"4"}. */
    public String canonical() {
        return any ? "*" : tags.stream().map(EntityTag::toString).collect(Collectors.joining(", "));
    }

    private static ApiProblemException invalid() {
        return ApiProblemException.of(ProblemCode.INVALID_IF_MATCH)
            .detail("If-Match must be * or a list of quoted entity tags; send the ETag exactly as received, "
                + "quotes included.")
            .error(ProblemError.atHeader(HEADER, "invalid", null));
    }
}
