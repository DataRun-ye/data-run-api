package org.nmcpye.datarun.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

/**
 * A stand-in for GlitchTip's envelope endpoint on the loopback interface. It answers every envelope the Sentry SDK
 * sends with 200 and keeps it, so a test can read exactly what would have left the server.
 */
final class FakeGlitchTip implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private final List<String> envelopes = new CopyOnWriteArrayList<>();

    private FakeGlitchTip(HttpServer server) {
        this.server = server;
    }

    static FakeGlitchTip start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            FakeGlitchTip glitchTip = new FakeGlitchTip(server);
            server.createContext("/", exchange -> {
                byte[] body = exchange.getRequestBody().readAllBytes();
                if ("gzip".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Content-Encoding"))) {
                    try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
                        body = gzip.readAllBytes();
                    }
                }
                glitchTip.envelopes.add(new String(body, StandardCharsets.UTF_8));
                byte[] answer = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, answer.length);
                exchange.getResponseBody().write(answer);
                exchange.close();
            });
            server.start();
            return glitchTip;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A DSN for project 1 on this server. */
    String dsn() {
        return "http://publickey@127.0.0.1:" + server.getAddress().getPort() + "/1";
    }

    /** Every envelope received so far, as sent (after gzip). */
    List<String> envelopes() {
        return List.copyOf(envelopes);
    }

    /** The events among the envelopes' items. */
    List<JsonNode> events() {
        List<JsonNode> events = new ArrayList<>();
        for (String envelope : envelopes) {
            String[] lines = envelope.split("\n");
            // Line 0 is the envelope header; then each item is a header line and a payload line.
            for (int i = 1; i + 1 < lines.length; i += 2) {
                try {
                    if ("event".equals(JSON.readTree(lines[i]).path("type").asText())) {
                        events.add(JSON.readTree(lines[i + 1]));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return events;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
