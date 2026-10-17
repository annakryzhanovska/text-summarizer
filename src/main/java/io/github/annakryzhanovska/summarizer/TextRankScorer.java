package io.github.annakryzhanovska.summarizer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TextRank (Mihalcea and Tarau, 2004): PageRank applied to sentences.
 *
 * <p>Every sentence is a node in a graph. Two sentences are connected by an edge whose weight is how many
 * words they share. A sentence is important when many other sentences are similar to it, in the same way
 * a web page is important when many pages link to it. The ranks are computed by repeating the PageRank
 * update until the values stop changing.
 */
public final class TextRankScorer implements SentenceScorer {

    static final double DAMPING = 0.85;
    static final double EPSILON = 1e-6;
    static final int MAX_ITERATIONS = 100;

    @Override
    public double[] score(List<Sentence> sentences) {
        int n = sentences.size();
        double[][] weight = new double[n][n];
        double[] weightSum = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double w = similarity(sentences.get(i).contentWords(), sentences.get(j).contentWords());
                weight[i][j] = w;
                weight[j][i] = w;
                weightSum[i] += w;
                weightSum[j] += w;
            }
        }

        double[] rank = new double[n];
        Arrays.fill(rank, 1.0);
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double[] next = new double[n];
            double change = 0;
            for (int i = 0; i < n; i++) {
                double incoming = 0;
                for (int j = 0; j < n; j++) {
                    if (weight[j][i] > 0) {
                        // sentence j passes on its rank, split by how strongly it is tied to each neighbour
                        incoming += weight[j][i] / weightSum[j] * rank[j];
                    }
                }
                next[i] = (1 - DAMPING) + DAMPING * incoming;
                change = Math.max(change, Math.abs(next[i] - rank[i]));
            }
            rank = next;
            if (change < EPSILON) {
                break;
            }
        }
        return rank;
    }

    /**
     * Similarity from the TextRank paper: number of shared words, divided by the log lengths of both
     * sentences so that long sentences are not favoured just for having more words.
     */
    static double similarity(List<String> a, List<String> b) {
        // log(1) is 0, so one-word sentences would divide by zero
        if (a.size() < 2 || b.size() < 2) {
            return 0;
        }
        Set<String> wordsOfB = new HashSet<>(b);
        long shared = new HashSet<>(a).stream().filter(wordsOfB::contains).count();
        return shared / (Math.log(a.size()) + Math.log(b.size()));
    }
}
