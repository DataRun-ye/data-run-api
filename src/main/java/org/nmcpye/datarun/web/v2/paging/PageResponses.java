package org.nmcpye.datarun.web.v2.paging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * The request's path and query with {@code cursor} set to the next page's. The query is kept as sent, pair by
     * pair, except that every character a URI cannot hold literally (a raw {@code [}, {@code |}, space or non-ASCII
     * character, a stray {@code %}) is percent-encoded, so the link is always a valid URI reference and the request
     * never fails for it; already encoded octets stay as they are.
     */
    static String nextLink(HttpServletRequest request, String next) {
        StringBuilder link = new StringBuilder(encodeIllegal(request.getRequestURI(), PATH));
        List<String> pairs = new ArrayList<>();
        String query = request.getQueryString();
        if (query != null) {
            for (String pair : query.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int equals = pair.indexOf('=');
                String name = equals < 0 ? pair : pair.substring(0, equals);
                if (!decoded(name).equals(PageQuery.CURSOR)) {
                    pairs.add(encodeIllegal(pair, QUERY));
                }
            }
        }
        pairs.add(PageQuery.CURSOR + "=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
        return link.append('?').append(String.join("&", pairs)).toString();
    }

    /** RFC 3986: unreserved, sub-delims, ':' and '@', with '/' in a path; '/' and '?' in a query. */
    private static final String PATH = "-._~!$&'()*+,;=:@/";
    private static final String QUERY = "-._~!$&'()*+,;=:@/?";

    private static String encodeIllegal(String text, String allowed) {
        StringBuilder out = new StringBuilder(text.length());
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            char c = (char) (bytes[i] & 0xFF);
            boolean letterOrDigit = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            boolean encodedOctet = c == '%' && i + 2 < bytes.length && isHex(bytes[i + 1]) && isHex(bytes[i + 2]);
            if (letterOrDigit || encodedOctet || (c < 0x80 && allowed.indexOf(c) >= 0)) {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", bytes[i] & 0xFF));
            }
        }
        return out.toString();
    }

    private static boolean isHex(byte b) {
        return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');
    }

    private static String decoded(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet
            ? servlet.getRequest() : null;
    }
}
