package com.engcoach.vocab;

/** Finds a Vietnamese meaning (and, for single words, the part of speech and pronunciation) for an English word. */
public interface WordLookupSource {

    /** Never throws: when nothing is found the result has null fields. {@code context} is the sentence it came from. */
    WordLookup find(String word, String context);
}
