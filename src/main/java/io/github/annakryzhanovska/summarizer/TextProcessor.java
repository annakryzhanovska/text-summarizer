package io.github.annakryzhanovska.summarizer;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Splits text into sentences and words. */
public final class TextProcessor {

    // \p{L} matches letters of any alphabet, so umlauts and ß stay part of a word
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TextProcessor() {
    }

    /**
     * Splits text into sentences with the JDK's locale-aware BreakIterator.
     * Line breaks inside a sentence are treated as spaces.
     */
    public static List<String> splitSentences(String text, Locale locale) {
        String normalized = WHITESPACE.matcher(text).replaceAll(" ").strip();
        List<String> sentences = new ArrayList<>();
        if (normalized.isEmpty()) {
            return sentences;
        }
        BreakIterator iterator = BreakIterator.getSentenceInstance(locale);
        iterator.setText(normalized);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = normalized.substring(start, end).strip();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    /** Lower-cased words without punctuation. */
    public static List<String> words(String text) {
        return NON_WORD.splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(word -> !word.isEmpty())
                .toList();
    }
}
