package io.github.annakryzhanovska.summarizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Word frequency scoring after H. P. Luhn (1958): words that appear often in a text describe its topic,
 * so sentences full of such words are important.
 *
 * <p>Each word gets its frequency divided by the highest frequency in the text (0..1).
 * A sentence scores the average of its words, so long sentences do not win just by being long.
 */
public final class FrequencyScorer implements SentenceScorer {

    // keep-simple: sentences with fewer content words than this score 0, so a two-word heading cannot
    // win by containing the most frequent word. Lower it if very short texts get empty summaries.
    static final int MIN_CONTENT_WORDS = 3;

    @Override
    public double[] score(List<Sentence> sentences) {
        Map<String, Integer> frequency = new HashMap<>();
        for (Sentence sentence : sentences) {
            for (String word : sentence.contentWords()) {
                frequency.merge(word, 1, Integer::sum);
            }
        }
        double maxFrequency = frequency.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        double[] scores = new double[sentences.size()];
        for (int i = 0; i < sentences.size(); i++) {
            List<String> words = sentences.get(i).contentWords();
            if (words.size() < MIN_CONTENT_WORDS) {
                continue;
            }
            double sum = 0;
            for (String word : words) {
                sum += frequency.get(word) / maxFrequency;
            }
            scores[i] = sum / words.size();
        }
        return scores;
    }
}
