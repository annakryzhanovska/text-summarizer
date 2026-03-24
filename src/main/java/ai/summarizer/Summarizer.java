package ai.summarizer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Summarizer {
    public static String summarize(String text, int sentenceCount) {

        List<Sentence> sentences = TextProcessor.splitIntoSentences(text)
                .stream()
                .map(Sentence::new)
                .collect(Collectors.toList());

        SentenceScorer.score(sentences);

        return sentences.stream()
                .sorted(Comparator.comparingInt(Sentence::getScore).reversed())
                .limit(sentenceCount)
                .map(Sentence::getText)
                .collect(Collectors.joining(" "));
    }
}
