package io.github.annakryzhanovska.summarizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummarizerTest {

    private static final String TEXT = """
            Coffee beans grow in tropical regions and coffee farmers harvest them by hand.
            Roasting coffee beans changes their taste and the colour of the beans.
            Tomorrow the weather will be rainy with strong wind near the coast.
            Many farmers sell coffee beans to roasting companies abroad.
            Fresh coffee tastes best a few days after the beans were roasted.
            """;

    private static SummaryOptions options(Method method, int sentences) {
        return new SummaryOptions(method, sentences, 0, null, 3);
    }

    @Test
    void keepsTheRequestedNumberOfSentences() {
        assertEquals(2, Summarizer.summarize(TEXT, options(Method.TEXTRANK, 2)).sentences().size());
    }

    @Test
    void keepsSentencesInTheirOriginalOrder() {
        for (Method method : Method.values()) {
            List<Integer> positions = Summarizer.summarize(TEXT, options(method, 3)).sentences().stream()
                    .map(Sentence::position).toList();
            assertEquals(positions.stream().sorted().toList(), positions, method + " changed the order");
        }
    }

    @Test
    void leavesOutTheOffTopicSentence() {
        Summary summary = Summarizer.summarize(TEXT, options(Method.TEXTRANK, 4));
        assertTrue(summary.sentences().stream().noneMatch(s -> s.text().contains("weather")));
    }

    @Test
    void ratioPicksAShareOfTheSentences() {
        SummaryOptions options = new SummaryOptions(Method.TEXTRANK, 3, 0.4, null, 0);
        assertEquals(2, Summarizer.summarize(TEXT, options).sentences().size());
    }

    @Test
    void neverReturnsMoreSentencesThanTheTextHas() {
        Summary summary = Summarizer.summarize("Only one sentence here.", options(Method.TEXTRANK, 10));
        assertEquals(1, summary.sentences().size());
        assertEquals(1, summary.compression(), 1e-9);
    }

    @Test
    void emptyTextGivesEmptySummary() {
        Summary summary = Summarizer.summarize("", SummaryOptions.defaults());
        assertTrue(summary.sentences().isEmpty());
        assertEquals("", summary.text());
        assertEquals(0, summary.originalReadingMinutes());
    }

    @Test
    void detectsLanguageAndFindsKeywords() {
        Summary summary = Summarizer.summarize(TEXT, options(Method.TEXTRANK, 2));
        assertEquals(Language.ENGLISH, summary.language());
        // "coffee" and "beans" both occur 5 times in 4 sentences, so they share the top score
        List<String> topTwo = summary.keywords().stream().limit(2).map(KeywordExtractor.Keyword::word).toList();
        assertEquals(List.of("beans", "coffee"), topTwo);
    }

    @Test
    void summarizesGermanText() {
        String german = "Die Stadt Stuttgart liegt in einem Tal. In der Stadt gibt es viele Weinberge. "
                + "Die Weinberge der Stadt sind im Herbst besonders schön. Gestern hat es geregnet.";
        Summary summary = Summarizer.summarize(german, options(Method.TEXTRANK, 2));
        assertEquals(Language.GERMAN, summary.language());
        assertTrue(summary.sentences().stream().noneMatch(s -> s.text().contains("geregnet")));
    }

    @Test
    void rejectsInvalidOptions() {
        assertThrows(IllegalArgumentException.class, () -> new SummaryOptions(Method.TEXTRANK, 0, 0, null, 5));
        assertThrows(IllegalArgumentException.class, () -> new SummaryOptions(Method.TEXTRANK, 3, 1.5, null, 5));
        assertThrows(IllegalArgumentException.class, () -> new SummaryOptions(Method.TEXTRANK, 3, 0, null, -1));
        assertThrows(IllegalArgumentException.class, () -> new SummaryOptions(null, 3, 0, null, 5));
    }
}
