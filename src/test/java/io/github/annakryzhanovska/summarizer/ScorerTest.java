package io.github.annakryzhanovska.summarizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScorerTest {

    // Three sentences about coffee and one unrelated sentence about the weather
    private static final List<Sentence> SENTENCES = List.of(
            sentence(0, "Coffee beans grow in tropical regions and coffee farmers harvest them by hand."),
            sentence(1, "Roasting coffee beans changes their taste and the colour of the beans."),
            sentence(2, "Tomorrow the weather will be rainy with strong wind near the coast."),
            sentence(3, "Many farmers sell coffee beans to roasting companies abroad."));

    private static Sentence sentence(int position, String text) {
        return Sentence.of(position, text, Language.ENGLISH);
    }

    @Test
    void textRankRanksTheOffTopicSentenceLowest() {
        double[] scores = new TextRankScorer().score(SENTENCES);
        assertEquals(2, indexOfMin(scores));
    }

    @Test
    void frequencyRanksTheOffTopicSentenceLowest() {
        double[] scores = new FrequencyScorer().score(SENTENCES);
        assertEquals(2, indexOfMin(scores));
    }

    @Test
    void textRankGivesAnIsolatedSentenceTheBaseScore() {
        double[] scores = new TextRankScorer().score(List.of(
                sentence(0, "Apples grow on trees in cold climates."),
                sentence(1, "Submarines dive deep below the ocean surface.")));
        assertEquals(1 - TextRankScorer.DAMPING, scores[0], 1e-9);
        assertEquals(1 - TextRankScorer.DAMPING, scores[1], 1e-9);
    }

    @Test
    void similarityCountsSharedWords() {
        double similarity = TextRankScorer.similarity(List.of("coffee", "beans", "farm"), List.of("coffee", "beans", "roast"));
        assertEquals(2 / (Math.log(3) + Math.log(3)), similarity, 1e-9);
    }

    @Test
    void similarityOfOneWordSentenceIsZero() {
        assertEquals(0, TextRankScorer.similarity(List.of("coffee"), List.of("coffee", "beans")));
    }

    @Test
    void scorersHandleNoSentences() {
        assertEquals(0, new TextRankScorer().score(List.of()).length);
        assertEquals(0, new FrequencyScorer().score(List.of()).length);
    }

    @Test
    void frequencyIgnoresVeryShortSentences() {
        double[] scores = new FrequencyScorer().score(List.of(sentence(0, "Coffee!"), sentence(1, "Coffee beans and coffee farmers.")));
        assertEquals(0, scores[0]);
        assertTrue(scores[1] > 0);
    }

    private static int indexOfMin(double[] values) {
        int min = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[min]) {
                min = i;
            }
        }
        return min;
    }
}
