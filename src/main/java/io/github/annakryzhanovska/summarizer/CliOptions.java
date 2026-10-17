package io.github.annakryzhanovska.summarizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Command line arguments. */
record CliOptions(SummaryOptions summaryOptions, OutputFormat format, List<Path> files,
                  Integer servePort, int threads, boolean help) {

    static final int DEFAULT_PORT = 8080;

    static final String USAGE = """
            Usage: summarizer [options] [file ...]

            Reads text from the given files, or from standard input if no file is given,
            and prints the most important sentences.

            Options:
              -n, --sentences N     number of sentences to keep (default 3)
              -r, --ratio R         share of sentences to keep instead, e.g. 0.3
              -m, --method M        textrank (default) or frequency
              -l, --lang L          de, en or auto (default auto)
              -k, --keywords N      number of keywords, 0 for none (default 5)
              -f, --format F        text (default), markdown or json
              -t, --threads N       parallel threads for several files (default: CPU cores)
                  --serve [PORT]    start the web interface and HTTP API (default port 8080)
              -h, --help            show this help
            """;

    static CliOptions parse(String[] args) {
        SummaryOptions defaults = SummaryOptions.defaults();
        Method method = defaults.method();
        int sentences = defaults.sentences();
        double ratio = defaults.ratio();
        Language language = defaults.language();
        int keywords = defaults.keywordCount();
        OutputFormat format = OutputFormat.TEXT;
        int threads = Runtime.getRuntime().availableProcessors();
        Integer port = null;
        List<Path> files = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> {
                    return new CliOptions(defaults, format, List.of(), null, threads, true);
                }
                case "-n", "--sentences" -> sentences = SummaryOptions.Parse.integer("sentences", value(args, ++i, arg));
                case "-r", "--ratio" -> ratio = SummaryOptions.Parse.ratio(value(args, ++i, arg));
                case "-m", "--method" -> method = SummaryOptions.Parse.method(value(args, ++i, arg));
                case "-l", "--lang" -> language = SummaryOptions.Parse.language(value(args, ++i, arg));
                case "-k", "--keywords" -> keywords = SummaryOptions.Parse.integer("keywords", value(args, ++i, arg));
                case "-f", "--format" -> format = OutputFormat.fromCode(value(args, ++i, arg));
                case "-t", "--threads" -> threads = SummaryOptions.Parse.integer("threads", value(args, ++i, arg));
                case "--serve" -> {
                    port = DEFAULT_PORT;
                    if (i + 1 < args.length && args[i + 1].matches("\\d+")) {
                        port = SummaryOptions.Parse.integer("port", args[++i]);
                    }
                    if (port > 65535) {
                        throw new IllegalArgumentException("port must be between 0 and 65535");
                    }
                }
                default -> {
                    if (arg.startsWith("-") && !arg.equals("-")) {
                        throw new IllegalArgumentException("Unknown option " + arg);
                    }
                    files.add(Path.of(arg));
                }
            }
        }
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1");
        }
        SummaryOptions options = new SummaryOptions(method, sentences, ratio, language, keywords);
        return new CliOptions(options, format, List.copyOf(files), port, threads, false);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[index];
    }
}
