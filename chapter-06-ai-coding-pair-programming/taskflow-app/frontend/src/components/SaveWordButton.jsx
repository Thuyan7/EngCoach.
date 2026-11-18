import { useState } from "react";
import { addWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";
import WordForm from "./WordForm.jsx";

// "Save a word" under a reviewed question: opens a small form and remembers which question the word came from.
export default function SaveWordButton({ questionId }) {
  const { token, signOut } = useAuth();
  const [open, setOpen] = useState(false);
  const [saved, setSaved] = useState("");

  async function save(fields) {
    try {
      await addWord(token, { ...fields, questionId });
    } catch (error) {
      if (error.status === 401) signOut();
      throw error;
    }
    setSaved(fields.word);
    setOpen(false);
  }

  return (
    <div className="save-word">
      {saved && <p className="body save-word-done" role="status">Saved "{saved}" to your vocabulary.</p>}
      {open ? (
        <WordForm submitLabel="Save word" onSubmit={save} onCancel={() => setOpen(false)} />
      ) : (
        <button type="button" className="link-btn" onClick={() => setOpen(true)}>
          {saved ? "Save another word" : "Save a word"}
        </button>
      )}
    </div>
  );
}
