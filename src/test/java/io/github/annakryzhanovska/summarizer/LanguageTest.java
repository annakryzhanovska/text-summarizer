package io.github.annakryzhanovska.summarizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageTest {

    @Test
    void detectsGerman() {
        List<String> words = TextProcessor.words("Das ist ein Satz, der auf Deutsch geschrieben ist und mit dem Leben zu tun hat.");
        assertEquals(Language.GERMAN, Language.detect(words));
    }

    @Test
    void detectsEnglish() {
        List<String> words = TextProcessor.words("This is a sentence that was written in English and it is about the weather.");
        assertEquals(Language.ENGLISH, Language.detect(words));
    }

    @Test
    void fallsBackToEnglishWithoutStopWords() {
        assertEquals(Language.ENGLISH, Language.detect(List.of("xyz", "qwerty")));
    }

    @Test
    void loadsStopWordLists() {
        assertTrue(Language.GERMAN.isStopWord("und"));
        assertTrue(Language.ENGLISH.isStopWord("the"));
        assertFalse(Language.GERMAN.isStopWord("wüste"));
    }

    @Test
    void parsesLanguageCodes() {
        assertEquals(Language.GERMAN, Language.fromCode("DE"));
        assertThrows(IllegalArgumentException.class, () -> Language.fromCode("fr"));
    }
}
