package ai.summarizer;

import java.util.Arrays;
import java.util.List;

//separating text on sentences
//looks for .!? and doesnt loose punctuatuins

public class TextProcessor {
    public static List<String> splitIntoSentences(String text) {
        return Arrays.asList(text.split("(?<=[.!?])\\s+"));
    }
}
