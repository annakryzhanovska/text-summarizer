package io.github.annakryzhanovska.summarizer;

import java.util.Locale;

/** The available scoring algorithms. */
public enum Method {
    TEXTRANK,
    FREQUENCY;

    public SentenceScorer scorer() {
        return switch (this) {
            case TEXTRANK -> new TextRankScorer();
            case FREQUENCY -> new FrequencyScorer();
        };
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Method fromCode(String code) {
        for (Method method : values()) {
            if (method.code().equalsIgnoreCase(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown method '" + code + "', use textrank or frequency");
    }
}
