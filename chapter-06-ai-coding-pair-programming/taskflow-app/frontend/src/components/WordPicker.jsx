import { useEffect, useRef, useState } from "react";
import { addWord, lookupWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";
import { cleanSelection, dictionaryUrl, sentenceAround } from "../vocabSelection.js";
import WordForm from "./WordForm.jsx";

// Wraps the text of a reviewed question so the learner can select a word or phrase they do not know and save it
// to their vocabulary. The sentence around the selection becomes the example and the meaning is looked up
// automatically, so one click saves the word; "Edit first" lets the learner change anything before saving.
// A "Save a word" button below lets them type any word by hand. `questionId` is stored with the word.
export default function WordPicker({ questionId, children }) {
  const { token, signOut } = useAuth();
  const wrapperRef = useRef(null);
  const [selection, setSelection] = useState(null); // { word, example }
  const [form, setForm] = useState(null); // null, or { word, example } to pre-fill (empty strings for a blank form)
  const [saved, setSaved] = useState("");
  const [suggestion, setSuggestion] = useState(null); // null while looking up, else { meaning, partOfSpeech, phonetic }
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  // Look the selected word up as soon as it is selected. A newer selection makes an older answer irrelevant.
  const selectedWord = selection ? selection.word : "";
  const selectedExample = selection ? selection.example : "";
  useEffect(() => {
    if (selectedWord === "") return undefined;
    let current = true;
    setSuggestion(null);
    setSaveError("");
    lookupWord(token, selectedWord, selectedExample)
      .then((found) => current && setSuggestion(found || {}))
      .catch(() => current && setSuggestion({}));
    return () => {
      current = false;
    };
  }, [selectedWord, selectedExample, token]);

  // Remember the last selection made inside this card. An empty selection is ignored on purpose: pressing the
  // Save button clears the browser selection before the click lands, and the button must still work.
  useEffect(() => {
    function onSelectionChange() {
      const picked = window.getSelection ? window.getSelection() : null;
      if (!picked || !wrapperRef.current) return;
      const word = cleanSelection(picked.toString());
      const anchor = picked.anchorNode;
      if (word === "" || !anchor || !wrapperRef.current.contains(anchor)) return;
      const element = anchor.nodeType === 1 ? anchor : anchor.parentElement;
      const paragraph = element ? element.closest("p") : null;
      setSelection({ word, example: paragraph ? sentenceAround(paragraph.textContent, word) : "" });
    }
    document.addEventListener("selectionchange", onSelectionChange);
    return () => document.removeEventListener("selectionchange", onSelectionChange);
  }, []);

  async function save(fields) {
    try {
      await addWord(token, { ...fields, questionId });
    } catch (error) {
      if (error.status === 401) signOut();
      throw error;
    }
    setSaved(fields.word);
    setForm(null);
    setSelection(null);
  }

  // One-click save with the looked-up meaning; without a meaning there is nothing to save, so open the form.
  async function saveSuggested() {
    if (!suggestion || !suggestion.meaning) {
      setForm({ word: selection.word, example: selection.example, meaning: "" });
      return;
    }
    setSaving(true);
    setSaveError("");
    try {
      await save({ word: selection.word, meaning: suggestion.meaning, example: selection.example });
    } catch (error) {
      setSaveError(error.message || "We couldn't save the word. Please try again.");
    }
    setSaving(false);
  }

  return (
    <div className="word-picker" ref={wrapperRef}>
      {children}

      <div className="save-word">
        {saved && <p className="body save-word-done" role="status">Saved "{saved}" to your vocabulary.</p>}

        {form ? (
          <WordForm
            key={`${form.word}|${form.example}|${form.meaning || ""}`}
            initial={{ word: form.word, meaning: form.meaning || "", example: form.example }}
            focusMeaning={form.word !== "" && !form.meaning}
            submitLabel="Save word"
            onSubmit={save}
            onCancel={() => setForm(null)}
          />
        ) : selection ? (
          <div className="selection-bar" role="group" aria-label="Selected text">
            <span>Selected: <strong>{selection.word}</strong></span>
            <span className="selection-meaning" aria-live="polite">
              {suggestion === null ? "Looking up the meaning…" : suggestion.meaning ? (
                <>
                  <em>{suggestion.meaning}</em>
                  {(suggestion.partOfSpeech || suggestion.phonetic) &&
                    <small> {[suggestion.partOfSpeech, suggestion.phonetic].filter(Boolean).join(" ")}</small>}
                </>
              ) : "No meaning found. You can type one."}
            </span>
            <button type="button" className="btn btn-primary" disabled={saving} onClick={saveSuggested}>
              {`Save "${selection.word}"`}
            </button>
            <button type="button" className="btn btn-secondary" disabled={saving}
              onClick={() => setForm({ word: selection.word, example: selection.example, meaning: suggestion?.meaning || "" })}>
              Edit first
            </button>
            <a href={dictionaryUrl(selection.word)} target="_blank" rel="noopener noreferrer">Look it up</a>
            <button type="button" className="link-btn" onClick={() => setSelection(null)}>Dismiss</button>
            {saveError && <div className="error-msg" role="alert">{saveError}</div>}
          </div>
        ) : (
          <>
            <p className="save-word-tip">Select a word or phrase in the text above to save it to your vocabulary.</p>
            <button type="button" className="link-btn" onClick={() => setForm({ word: "", example: "" })}>
              {saved ? "Save another word" : "Save a word"}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
