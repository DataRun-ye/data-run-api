package org.nmcpye.datarun.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;


@Component
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    /**
     * A client's id goes as it is into every log line of the request and into error reports, so only a short plain
     * one is kept (a UUID passes); anything else is replaced by a new id.
     */
    private static final Pattern CLIENT_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || !CLIENT_REQUEST_ID.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString();
        }


        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        // also expose it to downstream clients
        response.setHeader(REQUEST_ID_HEADER, requestId);


        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
}
