package io.github.annakryzhanovska.summarizer;

import java.util.List;

/** Rates how important each sentence is for the whole text. */
public interface SentenceScorer {

    /** Returns one score per sentence, in input order. A higher score means more important. */
    double[] score(List<Sentence> sentences);
}
