package ai.summarizer;

import java.util.List;
import java.util.Set;

public class SentenceScorer {
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "is", "and", "a", "to", "of", "in", "that", "it"
    );

    public static void score(List<Sentence> sentences) {
        for (Sentence sentence : sentences) {
            String[] words = sentence.getText()
                    .toLowerCase()
                    .replaceAll("[^a-z ]", "")
                    .split("\\s+");

            for (String word : words) {
                if (!STOP_WORDS.contains(word) && word.length() > 3) {
                    sentence.addScore(1);
                }
            }
        }
    }


}
