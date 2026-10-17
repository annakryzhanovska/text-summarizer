package io.github.annakryzhanovska.summarizer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliTest {

    private static final String TEXT = "Coffee beans grow in tropical regions. Coffee farmers harvest coffee beans by hand. "
            + "The weather is nice today. Roasted coffee beans smell wonderful.";

    @Test
    void parsesAllOptions() {
        CliOptions cli = CliOptions.parse(new String[]{"-n", "4", "-m", "frequency", "-l", "de", "-k", "2", "-f", "json", "-t", "2", "a.txt", "b.txt"});
        assertEquals(4, cli.summaryOptions().sentences());
        assertEquals(Method.FREQUENCY, cli.summaryOptions().method());
        assertEquals(Language.GERMAN, cli.summaryOptions().language());
        assertEquals(2, cli.summaryOptions().keywordCount());
        assertEquals(OutputFormat.JSON, cli.format());
        assertEquals(2, cli.threads());
        assertEquals(List.of(Path.of("a.txt"), Path.of("b.txt")), cli.files());
        assertNull(cli.servePort());
    }

    @Test
    void serveUsesDefaultOrGivenPort() {
        assertEquals(CliOptions.DEFAULT_PORT, CliOptions.parse(new String[]{"--serve"}).servePort());
        assertEquals(9000, CliOptions.parse(new String[]{"--serve", "9000"}).servePort());
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"--unknown"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"-n"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"-n", "three"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"-t", "0"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[]{"--serve", "70000"}));
    }

    @Test
    void readsStandardInputWhenNoFileIsGiven() {
        Run run = run(TEXT, "-n", "1");
        assertEquals(0, run.exitCode);
        assertTrue(run.out.contains("1 of 4 sentences"));
    }

    @Test
    void wrongArgumentsGiveExitCodeTwo() {
        Run run = run("", "--sentences", "0");
        assertEquals(2, run.exitCode);
        assertTrue(run.err.contains("sentences must be between"));
    }

    @Test
    void summarizesSeveralFilesInInputOrder() throws IOException {
        Path dir = Files.createTempDirectory("summarizer-test");
        Path first = Files.writeString(dir.resolve("first.txt"), TEXT);
        Path second = Files.writeString(dir.resolve("second.txt"), "Short text. Second file.");
        Run run = run("", "-n", "1", first.toString(), second.toString());
        assertEquals(0, run.exitCode);
        assertTrue(run.out.indexOf("== first.txt ==") < run.out.indexOf("== second.txt =="));
    }

    @Test
    void missingFileIsReportedButOthersStillRun() throws IOException {
        Path dir = Files.createTempDirectory("summarizer-test");
        Path good = Files.writeString(dir.resolve("good.txt"), TEXT);
        Run run = run("", good.toString(), dir.resolve("missing.txt").toString());
        assertEquals(1, run.exitCode);
        assertTrue(run.err.contains("missing.txt: file not found"));
        assertTrue(run.out.contains("== good.txt =="));
    }

    @Test
    void batchKeepsResultsInInputOrderWithManyThreads() throws Exception {
        Path dir = Files.createTempDirectory("summarizer-test");
        List<Path> files = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            files.add(Files.writeString(dir.resolve("file" + i + ".txt"), "File number " + i + " text. " + TEXT));
        }
        List<BatchSummarizer.Result> results = BatchSummarizer.summarizeAll(files, SummaryOptions.defaults(), 8);
        for (int i = 0; i < files.size(); i++) {
            assertEquals(files.get(i), results.get(i).file());
            assertFalse(results.get(i).failed());
        }
    }

    @Test
    void helpPrintsUsage() {
        Run run = run("", "--help");
        assertEquals(0, run.exitCode);
        assertTrue(run.out.startsWith("Usage: summarizer"));
    }

    private record Run(int exitCode, String out, String err) {
    }

    private static Run run(String stdin, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(args,
                new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
}
