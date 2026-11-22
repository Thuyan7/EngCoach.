import { useEffect, useRef, useState } from "react";
import { lookupWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";

// Form to save or edit one word. `onSubmit({ word, meaning, example })` must return a promise;
// if it rejects with an Error, the message is shown and the form stays open.
// `initial` with an id means editing a saved word; `initial` without an id only pre-fills a new word.
// For a new word the meaning is looked up while the learner types the word, so usually only the word is needed.
// The example sentence is optional and stays hidden until asked for (or when there already is one).
export default function WordForm({ initial, submitLabel, onSubmit, onCancel, focusMeaning = false }) {
  const { token } = useAuth();
  const isEdit = initial != null && initial.id != null;
  const [word, setWord] = useState(initial?.word ?? "");
  const [meaning, setMeaning] = useState(initial?.meaning ?? "");
  const [example, setExample] = useState(initial?.example ?? "");
  const [showExample, setShowExample] = useState(Boolean(initial?.example));
  const [hint, setHint] = useState([]); // part of speech and pronunciation of the looked-up word
  const [looking, setLooking] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const typedMeaning = useRef(Boolean(initial?.meaning)); // the learner wrote or kept a meaning: never overwrite it
  const wordInput = useRef(null);

  // Look the word up shortly after the learner stops typing it (new words only).
  useEffect(() => {
    const trimmed = word.trim();
    if (isEdit || trimmed.length < 2) {
      setHint([]);
      setLooking(false);
      return undefined;
    }
    let current = true;
    const timer = setTimeout(() => {
      setLooking(true);
      lookupWord(token, trimmed, example.trim())
        .then((found) => {
          if (!current) return;
          setHint([found?.partOfSpeech, found?.phonetic].filter(Boolean));
          if (found?.meaning && !typedMeaning.current) setMeaning(found.meaning);
        })
        .catch(() => {})
        .finally(() => current && setLooking(false));
    }, 600);
    return () => {
      current = false;
      clearTimeout(timer);
      setLooking(false);
    };
    // The example is only context for the lookup, so typing it does not start a new one.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [word, isEdit, token]);

  async function submit(event) {
    event.preventDefault();
    if (word.trim() === "" || meaning.trim() === "") {
      setError("Enter the word and its meaning.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await onSubmit({ word: word.trim(), meaning: meaning.trim(), example: example.trim() });
      if (!isEdit) {
        setWord("");
        setMeaning("");
        setExample("");
        setShowExample(false);
        setHint([]);
        typedMeaning.current = false;
        if (wordInput.current) wordInput.current.focus();
      }
    } catch (failure) {
      setError(failure.message || "We couldn't save the word. Please try again.");
    }
    setBusy(false);
  }

  return (
    <form className={`word-form${isEdit ? "" : " word-form-new"}`} onSubmit={submit}>
      <div className="word-form-row">
        <label>
          Word or phrase
          <input ref={wordInput} type="text" maxLength={100} value={word} placeholder="e.g. invoice"
            onChange={(e) => setWord(e.target.value)} />
        </label>
        <label>
          Meaning
          <input type="text" maxLength={500} value={meaning} autoFocus={focusMeaning}
            placeholder={looking ? "Looking up the meaning…" : isEdit ? "" : "Filled in for you, or type your own"}
            onChange={(e) => { typedMeaning.current = true; setMeaning(e.target.value); }} />
        </label>
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : submitLabel}</button>
        {onCancel && <button type="button" className="btn btn-secondary" disabled={busy} onClick={onCancel}>Cancel</button>}
      </div>
      {hint.length > 0 && (
        <p className="word-form-hint">{hint.map((item) => <span className="word-bar-chip" key={item}>{item}</span>)}</p>
      )}
      {showExample ? (
        <label>
          Example sentence (optional)
          <input type="text" maxLength={500} value={example} onChange={(e) => setExample(e.target.value)} />
        </label>
      ) : (
        <button type="button" className="link-btn word-form-add" onClick={() => setShowExample(true)}>+ Add an example sentence</button>
      )}
      {error && <div className="error-msg" role="alert">{error}</div>}
    </form>
  );
}
