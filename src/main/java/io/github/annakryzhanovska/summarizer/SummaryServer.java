package io.github.annakryzhanovska.summarizer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small HTTP server built on the JDK's own {@link HttpServer}, so it needs no framework.
 *
 * <ul>
 *   <li>{@code GET /} serves a web page with a text box</li>
 *   <li>{@code POST /api/summarize?sentences=3&method=textrank&lang=auto&keywords=5} takes the text as a
 *       UTF-8 request body and answers with JSON</li>
 * </ul>
 *
 * The server only listens on localhost, so it is not reachable from other computers.
 */
public final class SummaryServer {

    static final int MAX_BODY_BYTES = 1_000_000;

    private static final String PAGE = loadPage();

    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public SummaryServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::handlePage);
        server.createContext("/api/summarize", this::handleSummarize);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdown();
    }

    /** The actual port, useful when the server was started on port 0 (any free port). */
    public int port() {
        return server.getAddress().getPort();
    }

    private void handlePage(HttpExchange exchange) throws IOException {
        try {
            if (!"/".equals(exchange.getRequestURI().getPath())) {
                send(exchange, 404, "text/plain; charset=utf-8", "Not found");
            } else if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            } else {
                send(exchange, 200, "text/html; charset=utf-8", PAGE);
            }
        } finally {
            exchange.close();
        }
    }

    private void handleSummarize(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Use POST with the text as request body");
                return;
            }
            byte[] body = readAtMost(exchange.getRequestBody(), MAX_BODY_BYTES);
            if (body == null) {
                sendError(exchange, 413, "Text is larger than " + MAX_BODY_BYTES + " bytes");
                return;
            }
            String text = new String(body, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                sendError(exchange, 400, "Request body is empty");
                return;
            }
            SummaryOptions options;
            try {
                options = optionsFromQuery(exchange.getRequestURI().getRawQuery());
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
                return;
            }
            Summary summary = Summarizer.summarize(text, options);
            send(exchange, 200, "application/json; charset=utf-8", OutputFormat.JSON.render(summary, null));
        } finally {
            exchange.close();
        }
    }

    /** Reads the stream, or returns {@code null} if it holds more than {@code limit} bytes. */
    static byte[] readAtMost(InputStream in, int limit) throws IOException {
        byte[] data = in.readNBytes(limit + 1);
        return data.length > limit ? null : data;
    }

    static SummaryOptions optionsFromQuery(String rawQuery) {
        SummaryOptions defaults = SummaryOptions.defaults();
        Method method = defaults.method();
        int sentences = defaults.sentences();
        double ratio = defaults.ratio();
        Language language = defaults.language();
        int keywords = defaults.keywordCount();

        if (rawQuery != null && !rawQuery.isEmpty()) {
            for (String pair : rawQuery.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String key = decode(eq < 0 ? pair : pair.substring(0, eq));
                String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
                switch (key) {
                    case "sentences" -> sentences = SummaryOptions.Parse.integer("sentences", value);
                    case "ratio" -> ratio = SummaryOptions.Parse.ratio(value);
                    case "method" -> method = SummaryOptions.Parse.method(value);
                    case "lang" -> language = SummaryOptions.Parse.language(value);
                    case "keywords" -> keywords = SummaryOptions.Parse.integer("keywords", value);
                    default -> throw new IllegalArgumentException("Unknown parameter '" + key + "'");
                }
            }
        }
        return new SummaryOptions(method, sentences, ratio, language, keywords);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        send(exchange, status, "application/json; charset=utf-8",
                "{\"error\":" + OutputFormat.quote(message) + "}");
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String loadPage() {
        try (InputStream in = SummaryServer.class.getResourceAsStream("/web/index.html")) {
            if (in == null) {
                throw new IllegalStateException("Missing /web/index.html");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
