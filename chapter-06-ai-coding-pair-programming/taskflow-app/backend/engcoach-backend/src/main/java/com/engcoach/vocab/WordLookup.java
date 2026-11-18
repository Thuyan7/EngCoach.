package com.engcoach.vocab;

/** What we could find out about a word: any field is null when unknown. */
public record WordLookup(String meaning, String partOfSpeech, String phonetic) {

    public static WordLookup empty() {
        return new WordLookup(null, null, null);
    }
}
