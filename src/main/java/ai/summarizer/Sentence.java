package ai.summarizer;

public class Sentence {
    private final String text;
    private int score;

    public Sentence(String text) {
        this.text = text;
        this.score = 0;
    }

    public String getText() {
        return text;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int value) {
        this.score += value;
    }
}
