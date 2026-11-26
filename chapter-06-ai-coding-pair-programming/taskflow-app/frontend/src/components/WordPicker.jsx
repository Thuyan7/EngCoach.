import { useEffect, useRef, useState } from "react";
import { addWord, lookupWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";
import { cleanSelection, dictionaryUrl, sentenceAround } from "../vocabSelection.js";
import WordForm from "./WordForm.jsx";

// Wraps the text of a reviewed question so the learner can select a word or phrase they do not know and save it
// to their vocabulary. Selecting text opens a small bar at the bottom of the screen: the meaning is looked up
// automatically and can be corrected right there, and the sentence around the selection becomes the example.
// A "Save a word" button below lets them type any word by hand. `questionId` is stored with the word.
export default function WordPicker({ questionId, children }) {
  const { token, signOut } = useAuth();
  const wrapperRef = useRef(null);
  const [selection, setSelection] = useState(null); // { word, example }
  const [form, setForm] = useState(null); // null, or { word, example } to pre-fill (empty strings for a blank form)
  const [saved, setSaved] = useState("");
  const [suggestion, setSuggestion] = useState(null); // null while looking up, else { meaning, partOfSpeech, phonetic }
  const [meaning, setMeaning] = useState("");
  const typed = useRef(false); // the learner has edited the meaning, so a late lookup must not overwrite it
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  // Look the selected word up as soon as it is selected. A newer selection makes an older answer irrelevant.
  const selectedWord = selection ? selection.word : "";
  const selectedExample = selection ? selection.example : "";
  useEffect(() => {
    if (selectedWord === "") return undefined;
    let current = true;
    typed.current = false;
    setSuggestion(null);
    setMeaning("");
    setSaveError("");
    function arrived(found) {
      if (!current) return;
      setSuggestion(found || {});
      if (!typed.current && found && found.meaning) setMeaning(found.meaning);
    }
    lookupWord(token, selectedWord, selectedExample).then(arrived).catch(() => arrived({}));
    return () => {
      current = false;
    };
  }, [selectedWord, selectedExample, token]);

  // Remember the last selection made inside this card. An empty selection is ignored on purpose: pressing a
  // button clears the browser selection before the click lands, and the button must still work. Selecting
  // somewhere else closes this card's bar so only one bar is open at a time.
  useEffect(() => {
    function onSelectionChange() {
      const picked = window.getSelection ? window.getSelection() : null;
      if (!picked || !wrapperRef.current) return;
      const word = cleanSelection(picked.toString());
      const anchor = picked.anchorNode;
      if (word === "" || !anchor) return;
      const element = anchor.nodeType === 1 ? anchor : anchor.parentElement;
      if (!wrapperRef.current.contains(anchor)) {
        setSelection(null);
        return;
      }
      if (element && element.closest(".word-bar")) return;
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
    closeBar();
  }

  // Close the bar and drop the highlighted text, otherwise a later selection event would open the bar again.
  function closeBar() {
    setSelection(null);
    const picked = window.getSelection ? window.getSelection() : null;
    if (picked && typeof picked.removeAllRanges === "function") picked.removeAllRanges();
  }

  async function saveSelected(event) {
    event.preventDefault();
    if (saving) return;
    if (meaning.trim() === "") {
      setSaveError("Type the meaning first.");
      return;
    }
    setSaving(true);
    setSaveError("");
    try {
      await save({ word: selection.word, meaning: meaning.trim(), example: selection.example });
    } catch (error) {
      setSaveError(error.message || "We couldn't save the word. Please try again.");
    }
    setSaving(false);
  }

  const looking = selection && suggestion === null;
  const details = suggestion ? [suggestion.partOfSpeech, suggestion.phonetic].filter(Boolean) : [];

  return (
    <div className="word-picker" ref={wrapperRef}>
      {children}

      <div className="save-word">
        {saved && <p className="body save-word-done" role="status">Saved "{saved}" to your vocabulary.</p>}

        {form ? (
          <WordForm
            key={`${form.word}|${form.example}`}
            initial={{ word: form.word, meaning: "", example: form.example }}
            submitLabel="Save word"
            onSubmit={save}
            onCancel={() => setForm(null)}
          />
        ) : (
          <>
            <p className="save-word-tip">Select a word or phrase in the text above to save it to your vocabulary.</p>
            <button type="button" className="link-btn" onClick={() => { setSelection(null); setForm({ word: "", example: "" }); }}>
              {saved ? "Save another word" : "Save a word"}
            </button>
          </>
        )}
      </div>

      {selection && !form && (
        <form className="word-bar" role="group" aria-label="Save selected word" onSubmit={saveSelected}>
          <div className="word-bar-head">
            <strong className="word-bar-word">{selection.word}</strong>
            {details.map((detail) => <span className="word-bar-chip" key={detail}>{detail}</span>)}
            <a className="word-bar-link" href={dictionaryUrl(selection.word)} target="_blank" rel="noopener noreferrer">Look it up</a>
            <button type="button" className="word-bar-close" aria-label="Dismiss" onClick={closeBar}>×</button>
          </div>
          <div className="word-bar-row">
            <input
              type="text"
              className="word-bar-input"
              aria-label="Meaning"
              maxLength={500}
              value={meaning}
              placeholder={looking ? "Looking up the meaning…" : "Type the meaning"}
              onChange={(event) => { typed.current = true; setMeaning(event.target.value); }}
              onKeyDown={(event) => { if (event.key === "Escape") closeBar(); }}
            />
            <button type="submit" className="btn btn-primary" disabled={saving}>{`Save "${selection.word}"`}</button>
          </div>
          {selection.example && <p className="word-bar-example" title={selection.example}>{selection.example}</p>}
          {saveError && <div className="error-msg" role="alert">{saveError}</div>}
        </form>
      )}
    </div>
  );
}
