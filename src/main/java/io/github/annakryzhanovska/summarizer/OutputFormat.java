package io.github.annakryzhanovska.summarizer;

import java.util.Locale;
import java.util.stream.Collectors;

/** How a summary is printed. */
public enum OutputFormat {
    TEXT,
    MARKDOWN,
    JSON;

    public static OutputFormat fromCode(String code) {
        for (OutputFormat format : values()) {
            if (format.name().equalsIgnoreCase(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown format '" + code + "', use text, markdown or json");
    }

    /**
     * Renders a summary.
     *
     * @param title optional heading, for example the file name; may be {@code null}
     */
    public String render(Summary summary, String title) {
        return switch (this) {
            case TEXT -> text(summary, title);
            case MARKDOWN -> markdown(summary, title);
            case JSON -> json(summary, title);
        };
    }

    private static String text(Summary summary, String title) {
        StringBuilder out = new StringBuilder();
        if (title != null) {
            out.append("== ").append(title).append(" ==\n");
        }
        out.append(summary.text()).append("\n\n");
        if (!summary.keywords().isEmpty()) {
            out.append("Keywords: ").append(keywordList(summary)).append('\n');
        }
        out.append(statistics(summary)).append('\n');
        return out.toString();
    }

    private static String markdown(Summary summary, String title) {
        StringBuilder out = new StringBuilder();
        out.append("## ").append(title != null ? title : "Summary").append("\n\n");
        for (Sentence sentence : summary.sentences()) {
            out.append("- ").append(sentence.text()).append('\n');
        }
        if (!summary.keywords().isEmpty()) {
            out.append("\nKeywords: ").append(keywordList(summary)).append('\n');
        }
        out.append('\n').append(statistics(summary)).append('\n');
        return out.toString();
    }

    private static String json(Summary summary, String title) {
        String sentences = summary.sentences().stream()
                .map(s -> "{\"position\":" + s.position() + ",\"text\":" + quote(s.text()) + "}")
                .collect(Collectors.joining(","));
        String keywords = summary.keywords().stream()
                .map(k -> "{\"word\":" + quote(k.word()) + ",\"score\":" + round(k.score()) + "}")
                .collect(Collectors.joining(","));
        return "{"
                + (title != null ? "\"title\":" + quote(title) + "," : "")
                + "\"language\":\"" + summary.language().code() + "\","
                + "\"method\":\"" + summary.method().code() + "\","
                + "\"summary\":" + quote(summary.text()) + ","
                + "\"sentences\":[" + sentences + "],"
                + "\"keywords\":[" + keywords + "],"
                + "\"stats\":{"
                + "\"originalSentences\":" + summary.originalSentences() + ","
                + "\"summarySentences\":" + summary.sentences().size() + ","
                + "\"originalWords\":" + summary.originalWords() + ","
                + "\"summaryWords\":" + summary.words() + ","
                + "\"compression\":" + round(summary.compression()) + ","
                + "\"originalReadingMinutes\":" + summary.originalReadingMinutes()
                + "}}";
    }

    private static String keywordList(Summary summary) {
        return summary.keywords().stream().map(KeywordExtractor.Keyword::word).collect(Collectors.joining(", "));
    }

    private static String statistics(Summary summary) {
        return String.format(Locale.ROOT, "%d of %d sentences, %d of %d words (%d%%), language %s, method %s",
                summary.sentences().size(), summary.originalSentences(),
                summary.words(), summary.originalWords(), Math.round(summary.compression() * 100),
                summary.language().code(), summary.method().code());
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** Writes a string as a JSON string literal, escaping quotes, backslashes and control characters. */
    static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
