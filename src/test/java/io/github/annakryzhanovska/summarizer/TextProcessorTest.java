package io.github.annakryzhanovska.summarizer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextProcessorTest {

    @Test
    void splitsOnSentenceEndings() {
        List<String> sentences = TextProcessor.splitSentences("One. Two? Three!", Locale.ENGLISH);
        assertEquals(List.of("One.", "Two?", "Three!"), sentences);
    }

    @Test
    void joinsLineBreaksInsideASentence() {
        List<String> sentences = TextProcessor.splitSentences("This sentence\nis split\nacross lines.", Locale.ENGLISH);
        assertEquals(List.of("This sentence is split across lines."), sentences);
    }

    @Test
    void emptyTextHasNoSentences() {
        assertTrue(TextProcessor.splitSentences("   \n ", Locale.GERMAN).isEmpty());
    }

    @Test
    void wordsKeepUmlautsAndSharpS() {
        assertEquals(List.of("grüße", "aus", "der", "wüste"), TextProcessor.words("Grüße aus der Wüste!"));
    }

    @Test
    void wordsAreLowerCaseWithoutPunctuation() {
        assertEquals(List.of("hello", "world", "2026"), TextProcessor.words("Hello, World (2026)."));
    }
}
