package io.github.annakryzhanovska.summarizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputTest {

    private static final String TEXT = "Coffee beans grow in tropical regions. Coffee farmers harvest coffee beans by hand. "
            + "The weather is nice today.";

    @Test
    void jsonEscapesQuotesBackslashesAndControlCharacters() {
        assertEquals("\"say \\\"hi\\\"\\nC:\\\\temp\\t\\u0001\"", OutputFormat.quote("say \"hi\"\nC:\\temp\t\u0001"));
    }

    @Test
    void jsonContainsSummaryAndStats() {
        Summary summary = Summarizer.summarize(TEXT, new SummaryOptions(Method.TEXTRANK, 1, 0, null, 2));
        String json = OutputFormat.JSON.render(summary, "notes \"draft\".txt");
        assertTrue(json.startsWith("{\"title\":\"notes \\\"draft\\\".txt\""));
        assertTrue(json.contains("\"language\":\"en\""));
        assertTrue(json.contains("\"originalSentences\":3"));
        assertTrue(json.contains("\"summarySentences\":1"));
    }

    @Test
    void textShowsKeywordsAndStatistics() {
        Summary summary = Summarizer.summarize(TEXT, new SummaryOptions(Method.FREQUENCY, 1, 0, null, 2));
        String text = OutputFormat.TEXT.render(summary, null);
        assertTrue(text.contains("Keywords: coffee"));
        assertTrue(text.contains("1 of 3 sentences"));
        assertTrue(text.contains("method frequency"));
    }

    @Test
    void markdownListsEverySentence() {
        Summary summary = Summarizer.summarize(TEXT, new SummaryOptions(Method.TEXTRANK, 2, 0, null, 0));
        String markdown = OutputFormat.MARKDOWN.render(summary, "file.txt");
        assertTrue(markdown.startsWith("## file.txt"));
        assertEquals(2, markdown.lines().filter(line -> line.startsWith("- ")).count());
    }

    @Test
    void keywordsPreferTopicWords() {
        List<Sentence> sentences = List.of(
                Sentence.of(0, "Coffee beans grow in tropical regions.", Language.ENGLISH),
                Sentence.of(1, "Coffee farmers harvest beans by hand.", Language.ENGLISH),
                Sentence.of(2, "Coffee is roasted before it is sold.", Language.ENGLISH));
        List<KeywordExtractor.Keyword> keywords = KeywordExtractor.extract(sentences, 2);
        assertEquals(List.of("coffee", "beans"), keywords.stream().map(KeywordExtractor.Keyword::word).toList());
    }

    @Test
    void noKeywordsWhenLimitIsZero() {
        assertTrue(KeywordExtractor.extract(List.of(Sentence.of(0, "Coffee beans grow.", Language.ENGLISH)), 0).isEmpty());
    }

    @Test
    void unknownFormatIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> OutputFormat.fromCode("xml"));
    }
}
