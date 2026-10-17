package io.github.annakryzhanovska.summarizer;

/**
 * Settings for one summary.
 *
 * @param method       scoring algorithm
 * @param sentences    number of sentences to keep; ignored when {@code ratio} is set
 * @param ratio        share of sentences to keep (0 &lt; ratio &le; 1), or 0 to use {@code sentences}
 * @param language     language of the text, or {@code null} to detect it
 * @param keywordCount number of keywords to extract, 0 for none
 */
public record SummaryOptions(Method method, int sentences, double ratio, Language language, int keywordCount) {

    static final int MAX_SENTENCES = 200;
    static final int MAX_KEYWORDS = 50;

    public SummaryOptions {
        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }
        if (sentences < 1 || sentences > MAX_SENTENCES) {
            throw new IllegalArgumentException("sentences must be between 1 and " + MAX_SENTENCES);
        }
        if (ratio < 0 || ratio > 1 || Double.isNaN(ratio)) {
            throw new IllegalArgumentException("ratio must be between 0 and 1");
        }
        if (keywordCount < 0 || keywordCount > MAX_KEYWORDS) {
            throw new IllegalArgumentException("keywords must be between 0 and " + MAX_KEYWORDS);
        }
    }

    public static SummaryOptions defaults() {
        return new SummaryOptions(Method.TEXTRANK, 3, 0, null, 5);
    }

    /** How many sentences to keep from a text with {@code total} sentences. */
    int sentencesFor(int total) {
        int wanted = ratio > 0 ? (int) Math.max(1, Math.round(total * ratio)) : sentences;
        return Math.min(wanted, total);
    }

    /** Parses option values given as text, shared by the command line and the HTTP API. */
    static final class Parse {

        private Parse() {
        }

        static Method method(String value) {
            return Method.fromCode(value);
        }

        static Language language(String value) {
            return "auto".equalsIgnoreCase(value) ? null : Language.fromCode(value);
        }

        static int integer(String name, String value) {
            try {
                return Integer.parseInt(value.strip());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(name + " must be a whole number, got '" + value + "'");
            }
        }

        static double ratio(String value) {
            try {
                return Double.parseDouble(value.strip());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ratio must be a number like 0.3, got '" + value + "'");
            }
        }
    }
}
