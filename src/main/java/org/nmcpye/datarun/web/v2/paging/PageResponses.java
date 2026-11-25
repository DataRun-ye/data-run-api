package org.nmcpye.datarun.web.v2.paging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The response for a page. For a GET it also carries {@code Link: <...>; rel="next"} (RFC 8288) while a next page
 * exists: the request's own path and query with the cursor replaced, as a relative reference, so it holds behind
 * any proxy. A list read by POST (a search body) has only the body's {@code next}.
 */
public final class PageResponses {

    private PageResponses() {
    }

    public static <T> ResponseEntity<Page<T>> ok(Page<T> page) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        HttpServletRequest request = currentRequest();
        if (page.next() != null && request != null && "GET".equals(request.getMethod())) {
            response.header(HttpHeaders.LINK, "<" + nextLink(request, page.next()) + ">; rel=\"next\"");
        }
        return response.body(page);
    }

    /** The request's raw path and query with {@code cursor} set to the next page's. */
    static String nextLink(HttpServletRequest request, String next) {
        return UriComponentsBuilder.fromPath(request.getRequestURI())
            .query(request.getQueryString())
            .replaceQueryParam(PageQuery.CURSOR, next)
            .build(true)
            .toUriString();
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet
            ? servlet.getRequest() : null;
    }
}
