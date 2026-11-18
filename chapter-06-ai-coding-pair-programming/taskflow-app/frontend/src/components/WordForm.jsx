import { useState } from "react";

// Form to save or edit one word. `onSubmit({ word, meaning, example })` must return a promise;
// if it rejects with an Error, the message is shown and the form stays open.
export default function WordForm({ initial, submitLabel, onSubmit, onCancel }) {
  const [word, setWord] = useState(initial?.word ?? "");
  const [meaning, setMeaning] = useState(initial?.meaning ?? "");
  const [example, setExample] = useState(initial?.example ?? "");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

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
      if (!initial) {
        setWord("");
        setMeaning("");
        setExample("");
      }
    } catch (failure) {
      setError(failure.message || "We couldn't save the word. Please try again.");
    }
    setBusy(false);
  }

  return (
    <form className="word-form" onSubmit={submit}>
      <label>
        Word or phrase
        <input type="text" maxLength={100} value={word} onChange={(e) => setWord(e.target.value)} />
      </label>
      <label>
        Meaning
        <input type="text" maxLength={500} value={meaning} onChange={(e) => setMeaning(e.target.value)} />
      </label>
      <label>
        Example sentence (optional)
        <input type="text" maxLength={500} value={example} onChange={(e) => setExample(e.target.value)} />
      </label>
      {error && <div className="error-msg" role="alert">{error}</div>}
      <div className="q-actions">
        <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : submitLabel}</button>
        {onCancel && <button type="button" className="btn btn-secondary" disabled={busy} onClick={onCancel}>Cancel</button>}
      </div>
    </form>
  );
}
