package io.github.annakryzhanovska.summarizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Extractive summarizer: picks the most important sentences of a text and returns them unchanged,
 * in the order they appeared.
 */
public final class Summarizer {

    private Summarizer() {
    }

    public static Summary summarize(String text, SummaryOptions options) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(options, "options");

        List<String> allWords = TextProcessor.words(text);
        Language language = options.language() != null ? options.language() : Language.detect(allWords);

        List<String> rawSentences = TextProcessor.splitSentences(text, language.locale());
        List<Sentence> sentences = new ArrayList<>(rawSentences.size());
        for (int i = 0; i < rawSentences.size(); i++) {
            sentences.add(Sentence.of(i, rawSentences.get(i), language));
        }

        double[] scores = options.method().scorer().score(sentences);
        int keep = options.sentencesFor(sentences.size());

        List<Sentence> chosen = IntStream.range(0, sentences.size()).boxed()
                // best score first; on equal scores the earlier sentence wins
                .sorted(Comparator.comparingDouble((Integer i) -> scores[i]).reversed()
                        .thenComparingInt(i -> i))
                .limit(keep)
                .sorted()
                .map(sentences::get)
                .toList();

        List<KeywordExtractor.Keyword> keywords = KeywordExtractor.extract(sentences, options.keywordCount());
        return new Summary(chosen, keywords, language, options.method(), sentences.size(), allWords.size());
    }
}
