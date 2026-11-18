// Helpers for saving a word the learner selected in a script, passage or question.

const MAX_WORD_LENGTH = 100;
const MAX_EXAMPLE_LENGTH = 500;

// The selected text as one tidy line, or "" if it is empty or too long to be a word or phrase.
export function cleanSelection(text) {
  const tidy = String(text || "").replace(/\s+/g, " ").trim();
  if (tidy === "" || tidy.length > MAX_WORD_LENGTH) return "";
  return tidy;
}

// The sentence of `text` that contains `selected`, used as the example for the saved word.
// Returns "" if the selection is not found or the sentence is too long to store.
export function sentenceAround(text, selected) {
  const whole = String(text || "").replace(/\s+/g, " ").trim();
  const start = whole.toLowerCase().indexOf(String(selected || "").toLowerCase());
  if (start < 0 || selected === "") return "";

  let sentenceStart = 0;
  for (const piece of whole.split(/(?<=[.!?])\s+/)) {
    const sentenceEnd = sentenceStart + piece.length;
    if (start >= sentenceStart && start < sentenceEnd) {
      return piece.length <= MAX_EXAMPLE_LENGTH ? piece : "";
    }
    sentenceStart = sentenceEnd + 1; // the space the split removed
  }
  return "";
}

// A dictionary page for the word; opened in a new tab by the learner.
export function dictionaryUrl(word) {
  return `https://dictionary.cambridge.org/dictionary/english-vietnamese/${encodeURIComponent(word.toLowerCase())}`;
}
