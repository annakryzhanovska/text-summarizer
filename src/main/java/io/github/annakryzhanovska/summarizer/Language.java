package io.github.annakryzhanovska.summarizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Supported text languages, each with its own stop word list and sentence rules. */
public enum Language {
    GERMAN("de", Locale.GERMAN),
    ENGLISH("en", Locale.ENGLISH);

    private final String code;
    private final Locale locale;
    private final Set<String> stopWords;

    Language(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
        this.stopWords = loadStopWords(code);
    }

    public String code() {
        return code;
    }

    public Locale locale() {
        return locale;
    }

    public boolean isStopWord(String word) {
        return stopWords.contains(word);
    }

    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code.equalsIgnoreCase(code)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unsupported language '" + code + "', use de, en or auto");
    }

    /**
     * Guesses the language by counting how many words are German or English stop words.
     * Stop words make up a large share of any text, so this works well for a few sentences already.
     * On a tie English is returned.
     */
    public static Language detect(List<String> words) {
        long german = words.stream().filter(GERMAN::isStopWord).count();
        long english = words.stream().filter(ENGLISH::isStopWord).count();
        return german > english ? GERMAN : ENGLISH;
    }

    private static Set<String> loadStopWords(String code) {
        String resource = "/stopwords/" + code + ".txt";
        try (InputStream in = Language.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing stop word list " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
    }
}
