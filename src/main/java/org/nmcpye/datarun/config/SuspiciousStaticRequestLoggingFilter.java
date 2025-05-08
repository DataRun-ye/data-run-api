package org.nmcpye.datarun.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.security.IpBanService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Hamza Assada, <7amza.it@gmail.com>
 */

@Component
@Order(1)
@Slf4j
public class SuspiciousStaticRequestLoggingFilter implements Filter {
    private static final Pattern SUSPICIOUS_FILENAME = Pattern.compile(
        "(?i).*\\.(git|env|aws|bak|swp|idea|vscode|pem|pub|key|log|cfg|config|credentials).*" +
            "|.*\\.\\..*|.*\\.DS_Store|Thumbs\\.db"
    );

    private static final Pattern STATIC_EXTENSIONS = Pattern.compile(".*\\.(json|xml|config|bak|env|git|zip|tar|gz|rar|pem|key).*", Pattern.CASE_INSENSITIVE);

    private final IpBanService ipBanService;
    private final MeterRegistry meterRegistry;

    // constructor injection
    public SuspiciousStaticRequestLoggingFilter(IpBanService ipBanService, MeterRegistry meterRegistry) {
        this.ipBanService = ipBanService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String ip = httpRequest.getRemoteAddr();

        if (ipBanService.isBanned(ip)) {
            log.warn("⛔ Blocked banned IP: {}", ip);
            response.getWriter().write("Access Denied.");
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Log and detect suspicious file path requests

        if (uri.contains("..")) {
            log.warn("🔓 Path traversal attempt from {}: {}", ip, uri);
            ipBanService.registerSuspiciousActivity(ip);
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (STATIC_EXTENSIONS.matcher(uri).matches()) {
            String fileName = uri.substring(uri.lastIndexOf('/') + 1);
            if (SUSPICIOUS_FILENAME.matcher(fileName).matches()) {
                log.warn("🚨 Suspicious filename access attempt from IP {}: {}", ip, uri);
                ipBanService.registerSuspiciousActivity(ip);

                Counter.builder("suspicious_static_requests")
                    .tag("ip", ip)
                    .tag("uri", uri)
                    .register(meterRegistry)
                    .increment();
            }
        }


        chain.doFilter(request, response);
    }
}

