package io.github.annakryzhanovska.summarizer;

import java.util.List;

/**
 * One sentence of the input text.
 *
 * @param position     index in the original text, used to restore the original order
 * @param text         the sentence as written
 * @param contentWords lower-cased words without stop words; the scorers only look at these
 */
public record Sentence(int position, String text, List<String> contentWords) {

    public Sentence {
        contentWords = List.copyOf(contentWords);
    }

    /** Builds a sentence and keeps only the words that carry meaning in the given language. */
    public static Sentence of(int position, String text, Language language) {
        List<String> content = TextProcessor.words(text).stream()
                .filter(word -> word.length() > 1)
                .filter(word -> !language.isStopWord(word))
                .filter(word -> !word.chars().allMatch(Character::isDigit))
                .toList();
        return new Sentence(position, text, content);
    }
}
