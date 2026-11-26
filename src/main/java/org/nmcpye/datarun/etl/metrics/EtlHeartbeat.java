package org.nmcpye.datarun.etl.metrics;

import org.nmcpye.datarun.etl.config.EtlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Tells a heartbeat monitor that the ETL is alive: after a poll that succeeded, it sends a POST to
 * {@code datarun.etl.heartbeat-url}, at most once a minute. A monitor that hears nothing for its interval reports
 * the ETL down, so it notices an ETL that stopped polling or keeps failing, which no error line shows. Production
 * points it at a GlitchTip "Heartbeat" uptime monitor, whose check accepts only POST. An empty URL turns it off.
 * <p>
 * While the submission feed is on, the poll pings only while the feed is within its freshness target
 * ({@code ScheduledOrchestratorRunner}), so the monitor also reports a feed that fell behind or gave up on a change.
 * Until tall and events retire this is the ETL's heartbeat, which the feed's freshness also gates; once they retire, it
 * is the feed's heartbeat.
 * <p>
 * Nothing here can slow, block or fail a poll: the request goes out on the HTTP client's own threads and gives up
 * after {@link #TIMEOUT}, a failure is logged at DEBUG only, and {@link #pollSucceeded()} never throws. The URL is
 * never logged: it is the monitor's credential.
 */
@Component
public class EtlHeartbeat {

    static final Duration MIN_INTERVAL = Duration.ofMinutes(1);
    static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final Logger log = LoggerFactory.getLogger(EtlHeartbeat.class);

    private static final long NEVER = Long.MIN_VALUE;

    private final HttpClient client;
    private final HttpRequest ping;
    private final LongSupplier nanoTime;
    private final AtomicLong lastPing = new AtomicLong(NEVER);

    @Autowired
    public EtlHeartbeat(EtlProperties properties) {
        this(properties.getHeartbeatUrl(), TIMEOUT, System::nanoTime);
    }

    EtlHeartbeat(String url, Duration timeout, LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
        URI uri = monitorUri(url);
        if (uri == null) {
            this.client = null;
            this.ping = null;
            return;
        }
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build();
        this.ping = HttpRequest.newBuilder(uri).timeout(timeout).POST(HttpRequest.BodyPublishers.noBody()).build();
        log.info("The ETL pings its heartbeat monitor after a successful poll, at most once a minute");
    }

    /**
     * Called after each poll that succeeded: pings the monitor unless it was pinged less than a minute ago.
     * Returns at once and never throws.
     */
    public void pollSucceeded() {
        if (ping == null) {
            return;
        }
        try {
            long now = nanoTime.getAsLong();
            long last = lastPing.get();
            if (last != NEVER && now - last < MIN_INTERVAL.toNanos()) {
                return;
            }
            if (!lastPing.compareAndSet(last, now)) {
                return;
            }
            client.sendAsync(ping, HttpResponse.BodyHandlers.discarding()).whenComplete(EtlHeartbeat::answered);
        } catch (RuntimeException e) {
            log.debug("The ETL heartbeat was not sent: {}", e.toString());
        }
    }

    private static void answered(HttpResponse<Void> response, Throwable failure) {
        if (failure != null) {
            Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause() : failure;
            log.debug("The ETL heartbeat failed: {}", cause.toString());
        } else if (response.statusCode() >= 300) {
            log.debug("The ETL heartbeat monitor answered HTTP {}", response.statusCode());
        }
    }

    /** The monitor's address, or null when none is set or the setting is not an http(s) URL. */
    private static URI monitorUri(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) && uri.getHost() != null) {
                return uri;
            }
        } catch (URISyntaxException e) {
            // reported below, without the URL
        }
        log.warn("datarun.etl.heartbeat-url is not an http(s) URL; the ETL pings no heartbeat monitor");
        return null;
    }
}
