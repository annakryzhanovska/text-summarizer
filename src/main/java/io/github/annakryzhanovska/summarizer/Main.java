package io.github.annakryzhanovska.summarizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/** Command line entry point. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.in, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Runs the program and returns the exit code: 0 success, 1 a file failed, 2 wrong arguments. */
    static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        CliOptions cli;
        try {
            cli = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.println("Run with --help for usage.");
            return 2;
        }
        if (cli.help()) {
            out.print(CliOptions.USAGE);
            return 0;
        }

        try {
            if (cli.servePort() != null) {
                SummaryServer server = new SummaryServer(cli.servePort());
                server.start();
                out.println("Summarizer running on http://localhost:" + server.port() + " (Ctrl+C to stop)");
                return 0; // the server threads keep the program running
            }
            if (cli.files().isEmpty()) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                out.print(cli.format().render(Summarizer.summarize(text, cli.summaryOptions()), null));
                return 0;
            }
            return summarizeFiles(cli, out, err);
        } catch (IOException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            err.println("Interrupted");
            return 1;
        }
    }

    private static int summarizeFiles(CliOptions cli, PrintStream out, PrintStream err) throws InterruptedException {
        List<BatchSummarizer.Result> results =
                BatchSummarizer.summarizeAll(cli.files(), cli.summaryOptions(), cli.threads());
        boolean anyFailed = false;
        for (BatchSummarizer.Result result : results) {
            if (result.failed()) {
                err.println("Error: " + result.error());
                anyFailed = true;
            }
        }
        List<BatchSummarizer.Result> ok = results.stream().filter(r -> !r.failed()).toList();
        boolean showTitles = cli.files().size() > 1;

        if (cli.format() == OutputFormat.JSON && showTitles) {
            out.println(ok.stream()
                    .map(r -> OutputFormat.JSON.render(r.summary(), r.file().getFileName().toString()))
                    .collect(Collectors.joining(",\n", "[\n", "\n]")));
        } else {
            for (BatchSummarizer.Result r : ok) {
                String title = showTitles ? r.file().getFileName().toString() : null;
                out.print(cli.format().render(r.summary(), title));
                if (showTitles) {
                    out.println();
                }
            }
        }
        return anyFailed ? 1 : 0;
    }
}
