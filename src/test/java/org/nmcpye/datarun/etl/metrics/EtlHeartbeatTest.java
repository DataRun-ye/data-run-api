package org.nmcpye.datarun.etl.metrics;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** The ETL's heartbeat ping, against a monitor on the loopback interface and a clock the test moves. */
class EtlHeartbeatTest {

    private static final String PATH =
        "/api/0/organizations/example-org/heartbeat_check/0b7c9a52-3a4e-4a8e-9a5f-2c1d6f0e8b11/";
    /** Production uses 5 s; much less fails the first connection of a cold JVM. */
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final BlockingQueue<String> pings = new LinkedBlockingQueue<>();
    private final AtomicInteger answer = new AtomicInteger(200);
    private final AtomicLong now = new AtomicLong();
    private volatile Duration answerAfter = Duration.ZERO;
    private HttpServer monitor;
    private ExecutorService monitorThreads;
    private ListAppender<ILoggingEvent> logged;
    private Level levelBefore;

    @BeforeEach
    void startMonitor() throws IOException {
        monitorThreads = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "heartbeat-monitor");
            thread.setDaemon(true);
            return thread;
        });
        monitor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        monitor.setExecutor(monitorThreads);
        monitor.createContext("/", exchange -> {
            pings.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            exchange.getRequestBody().readAllBytes();
            try {
                Thread.sleep(answerAfter.toMillis());
                exchange.sendResponseHeaders(answer.get(), -1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        monitor.start();
        logged = new ListAppender<>();
        logged.start();
        levelBefore = heartbeatLogger().getLevel();
        heartbeatLogger().setLevel(Level.DEBUG);
        heartbeatLogger().addAppender(logged);
    }

    @AfterEach
    void stopMonitor() {
        heartbeatLogger().detachAppender(logged);
        heartbeatLogger().setLevel(levelBefore);
        monitor.stop(0);
        monitorThreads.shutdownNow();
    }

    @Test
    void aSuccessfulPollPingsTheMonitorWithAPost() throws InterruptedException {
        heartbeat(monitorUrl()).pollSucceeded();

        assertThat(pings.poll(5, TimeUnit.SECONDS)).isEqualTo("POST " + PATH);
        assertThat(messages(Level.INFO))
            .containsExactly("The ETL pings its heartbeat monitor after a successful poll, at most once a minute");
    }

    @Test
    void theMonitorIsPingedAtMostOnceAMinute() throws InterruptedException {
        EtlHeartbeat heartbeat = heartbeat(monitorUrl());

        for (long second : new long[]{0, 30, 59, 60, 61, 119, 120, 150}) {
            now.set(TimeUnit.SECONDS.toNanos(second));
            heartbeat.pollSucceeded();
        }

        assertThat(receive(3)).as("pings at 0, 60 and 120 s").hasSize(3);
        assertThat(pings.poll(500, TimeUnit.MILLISECONDS)).as("polls at 30, 59, 61, 119 and 150 s ping nothing")
            .isNull();
    }

    @Test
    void aMonitorThatAnswersWithAnErrorChangesNothingForThePoll() throws InterruptedException {
        answer.set(500);
        EtlHeartbeat heartbeat = heartbeat(monitorUrl());

        assertThatCode(heartbeat::pollSucceeded).doesNotThrowAnyException();
        now.set(TimeUnit.SECONDS.toNanos(60));
        assertThatCode(heartbeat::pollSucceeded).doesNotThrowAnyException();

        assertThat(receive(2)).as("a refused ping does not stop the next one").hasSize(2);
        assertThat(eventually(Level.DEBUG, "The ETL heartbeat monitor answered HTTP 500")).isTrue();
        assertThat(warningsOrWorse()).isEmpty();
    }

    @Test
    void anUnreachableMonitorChangesNothingForThePoll() throws IOException, InterruptedException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        EtlHeartbeat heartbeat = heartbeat("http://127.0.0.1:" + closedPort + PATH);

        assertReturnsAtOnce(heartbeat);
        assertThat(eventually(Level.DEBUG, "The ETL heartbeat failed: java.net.ConnectException")).isTrue();
        assertThat(warningsOrWorse()).isEmpty();
    }

    @Test
    void aMonitorThatDoesNotAnswerDoesNotHoldThePoll() throws InterruptedException {
        answerAfter = Duration.ofSeconds(3);
        EtlHeartbeat heartbeat = heartbeat(monitorUrl());

        assertReturnsAtOnce(heartbeat);
        assertThat(pings.poll(5, TimeUnit.SECONDS)).as("the ping went out").isNotNull();
        assertThat(eventually(Level.DEBUG, "The ETL heartbeat failed: java.net.http.HttpTimeoutException"))
            .as("it gives up after its timeout").isTrue();

        now.set(TimeUnit.SECONDS.toNanos(60));
        assertReturnsAtOnce(heartbeat);
        assertThat(pings.poll(5, TimeUnit.SECONDS)).as("the next one goes out too").isNotNull();
        assertThat(warningsOrWorse()).isEmpty();
    }

    @Test
    void nothingThatFailsWhilePingingReachesThePoll() {
        EtlHeartbeat heartbeat = new EtlHeartbeat(monitorUrl(), TIMEOUT, () -> {
            throw new IllegalStateException("clock failure");
        });

        assertThatCode(heartbeat::pollSucceeded).doesNotThrowAnyException();
        assertThat(messages(Level.DEBUG)).singleElement().asString().startsWith("The ETL heartbeat was not sent");
    }

    @Test
    void anEmptyUrlTurnsTheHeartbeatOff() throws InterruptedException {
        EtlHeartbeat heartbeat = new EtlHeartbeat(new EtlProperties());

        assertThatCode(heartbeat::pollSucceeded).doesNotThrowAnyException();
        synchronized (logged) {
            assertThat(logged.list).as("no startup line, no ping").isEmpty();
        }
        assertThat(pings.poll(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void aUrlThatIsNotHttpTurnsTheHeartbeatOffWithAWarningThatHidesIt() {
        EtlHeartbeat heartbeat = heartbeat("monitor.invalid" + PATH);

        assertThatCode(heartbeat::pollSucceeded).doesNotThrowAnyException();
        assertThat(warningsOrWorse()).singleElement().asString()
            .isEqualTo("datarun.etl.heartbeat-url is not an http(s) URL; the ETL pings no heartbeat monitor");
    }

    /** Compose passes DATARUN_ETL_HEARTBEAT_URL; relaxed binding must take it to the setting. */
    @Test
    void theComposeVariableReachesTheSetting() {
        SystemEnvironmentPropertySource environment = new SystemEnvironmentPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            Map.of("DATARUN_ETL_HEARTBEAT_URL", "https://monitor.invalid" + PATH));

        EtlProperties properties = new Binder(ConfigurationPropertySources.from(environment))
            .bind("datarun.etl", EtlProperties.class).get();

        assertThat(properties.getHeartbeatUrl()).isEqualTo("https://monitor.invalid" + PATH);
    }

    private EtlHeartbeat heartbeat(String url) {
        return new EtlHeartbeat(url, TIMEOUT, now::get);
    }

    private String monitorUrl() {
        return "http://127.0.0.1:" + monitor.getAddress().getPort() + PATH;
    }

    /** Well under the 2 s a ping that waited for the monitor would take. */
    private static void assertReturnsAtOnce(EtlHeartbeat heartbeat) {
        long started = System.nanoTime();
        assertThatCode(heartbeat::pollSucceeded).doesNotThrowAnyException();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(500));
    }

    private List<String> receive(int count) throws InterruptedException {
        List<String> received = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String ping = pings.poll(5, TimeUnit.SECONDS);
            if (ping == null) {
                break;
            }
            received.add(ping);
        }
        return received;
    }

    /** Lines logged at exactly this level; the HTTP client's threads append while the test reads. */
    private List<String> messages(Level level) {
        synchronized (logged) {
            return logged.list.stream().filter(event -> event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage).toList();
        }
    }

    private List<String> warningsOrWorse() {
        synchronized (logged) {
            return logged.list.stream().filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .map(ILoggingEvent::getFormattedMessage).toList();
        }
    }

    /** Whether a line at this level starting with this text arrives within five seconds. */
    private boolean eventually(Level level, String start) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (messages(level).stream().anyMatch(message -> message.startsWith(start))) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    private static Logger heartbeatLogger() {
        return (Logger) LoggerFactory.getLogger(EtlHeartbeat.class);
    }
}
