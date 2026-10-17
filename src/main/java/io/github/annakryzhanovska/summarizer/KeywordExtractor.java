package io.github.annakryzhanovska.summarizer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Finds the words that best describe a text with TF-IDF, treating every sentence as a small document.
 *
 * <p>TF (term frequency) rewards words that appear often. IDF (inverse document frequency) lowers the
 * score of words that appear in almost every sentence, because they say little about the topic.
 */
public final class KeywordExtractor {

    private KeywordExtractor() {
    }

    public record Keyword(String word, double score) {
    }

    public static List<Keyword> extract(List<Sentence> sentences, int limit) {
        if (limit <= 0 || sentences.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> termFrequency = new HashMap<>();
        Map<String, Integer> sentenceFrequency = new HashMap<>();
        for (Sentence sentence : sentences) {
            for (String word : sentence.contentWords()) {
                termFrequency.merge(word, 1, Integer::sum);
            }
            for (String word : new HashSet<>(sentence.contentWords())) {
                sentenceFrequency.merge(word, 1, Integer::sum);
            }
        }
        double n = sentences.size();
        return termFrequency.entrySet().stream()
                .filter(entry -> entry.getKey().length() > 2)
                // the +1 keeps a word that occurs in every sentence from scoring exactly 0
                .map(entry -> new Keyword(entry.getKey(),
                        entry.getValue() * Math.log(1 + n / sentenceFrequency.get(entry.getKey()))))
                .sorted(Comparator.comparingDouble(Keyword::score).reversed().thenComparing(Keyword::word))
                .limit(limit)
                .toList();
    }
}
