package io.github.annakryzhanovska.summarizer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The result of summarizing one text.
 *
 * @param sentences         chosen sentences in their original order
 * @param keywords          most descriptive words of the whole text
 * @param language          language that was used (given or detected)
 * @param method            algorithm that was used
 * @param originalSentences number of sentences in the input
 * @param originalWords     number of words in the input
 */
public record Summary(List<Sentence> sentences, List<KeywordExtractor.Keyword> keywords, Language language,
                      Method method, int originalSentences, int originalWords) {

    static final int WORDS_PER_MINUTE = 200;

    public Summary {
        sentences = List.copyOf(sentences);
        keywords = List.copyOf(keywords);
    }

    public String text() {
        return sentences.stream().map(Sentence::text).collect(Collectors.joining(" "));
    }

    public int words() {
        return sentences.stream().mapToInt(s -> TextProcessor.words(s.text()).size()).sum();
    }

    /** Length of the summary as a share of the original, from 0 to 1. */
    public double compression() {
        return originalWords == 0 ? 0 : (double) words() / originalWords;
    }

    /** Reading time of the original text in whole minutes, at least 1 for any non-empty text. */
    public int originalReadingMinutes() {
        return originalWords == 0 ? 0 : (int) Math.ceil((double) originalWords / WORDS_PER_MINUTE);
    }
}
