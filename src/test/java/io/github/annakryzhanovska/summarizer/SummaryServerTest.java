package io.github.annakryzhanovska.summarizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryServerTest {

    private static final String TEXT = "Coffee beans grow in tropical regions. Coffee farmers harvest coffee beans by hand. "
            + "The weather is nice today.";

    private final HttpClient client = HttpClient.newHttpClient();
    private SummaryServer server;

    @BeforeEach
    void start() throws IOException {
        server = new SummaryServer(0);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void summarizesPostedText() throws Exception {
        HttpResponse<String> response = post("/api/summarize?sentences=1&keywords=2", TEXT);
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertTrue(response.body().contains("\"summarySentences\":1"));
    }

    @Test
    void servesTheWebPage() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<title>Text summarizer</title>"));
    }

    @Test
    void rejectsEmptyBody() throws Exception {
        assertEquals(400, post("/api/summarize", "  ").statusCode());
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        HttpResponse<String> response = post("/api/summarize?sentences=abc", TEXT);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("whole number"));
        assertEquals(400, post("/api/summarize?colour=red", TEXT).statusCode());
    }

    @Test
    void rejectsTooLargeBody() throws Exception {
        assertEquals(413, post("/api/summarize", "a".repeat(SummaryServer.MAX_BODY_BYTES + 1)).statusCode());
    }

    @Test
    void rejectsGetOnApi() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/api/summarize")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    void readAtMostStopsAtTheLimit() throws IOException {
        assertEquals(3, SummaryServer.readAtMost(new ByteArrayInputStream(new byte[3]), 3).length);
        assertNull(SummaryServer.readAtMost(new ByteArrayInputStream(new byte[4]), 3));
    }

    @Test
    void queryParametersAreDecoded() {
        SummaryOptions options = SummaryServer.optionsFromQuery("sentences=2&method=frequency&lang=de&ratio=0.5");
        assertEquals(2, options.sentences());
        assertEquals(Method.FREQUENCY, options.method());
        assertEquals(Language.GERMAN, options.language());
        assertEquals(0.5, options.ratio());
        assertThrows(IllegalArgumentException.class, () -> SummaryServer.optionsFromQuery("method=magic"));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        // the server binds the loopback address; "localhost" can resolve to IPv6 first on some machines
        return URI.create("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.port() + path);
    }
}
