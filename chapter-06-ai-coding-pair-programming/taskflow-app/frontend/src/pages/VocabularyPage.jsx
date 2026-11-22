import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { addWord, deleteWord, listWords, updateWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";
import WordForm from "../components/WordForm.jsx";

// Vocabulary notebook: save words, edit or delete them, and start the flashcard review.
export default function VocabularyPage() {
  const { token, signOut } = useAuth();
  const [data, setData] = useState(null);
  const [status, setStatus] = useState("loading");
  const [editingId, setEditingId] = useState(null);
  const [actionError, setActionError] = useState("");

  const load = useCallback(async () => {
    try {
      setData(await listWords(token));
      setStatus("ready");
    } catch (error) {
      if (error.status === 401) {
        signOut();
        return;
      }
      setStatus("error");
    }
  }, [token, signOut]);

  useEffect(() => {
    load();
  }, [load]);

  // A 409 means the word is already saved; its message is shown by the form.
  async function handleAdd(fields) {
    try {
      await addWord(token, fields);
    } catch (error) {
      if (error.status === 401) signOut();
      throw error;
    }
    await load();
  }

  async function handleEdit(id, fields) {
    try {
      await updateWord(token, id, fields);
    } catch (error) {
      if (error.status === 401) signOut();
      throw error;
    }
    setEditingId(null);
    await load();
  }

  async function handleDelete(word) {
    if (!window.confirm(`Delete "${word.word}" from your vocabulary?`)) return;
    setActionError("");
    try {
      await deleteWord(token, word.id);
      await load();
    } catch (error) {
      if (error.status === 401) {
        signOut();
        return;
      }
      setActionError("We couldn't delete the word. Please try again.");
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>Vocabulary</h1>
        <p>Save words you want to remember, then study them as flashcards. Words you forget come back sooner.</p>
      </div>

      {status === "loading" && <p role="status">Loading your vocabulary…</p>}

      {status === "error" && (
        <div>
          <div className="error-msg" role="alert">We couldn't load your vocabulary. Please try again.</div>
          <button type="button" className="btn btn-secondary" onClick={load}>Try again</button>
        </div>
      )}

      {status === "ready" && (
        <>
          <section className="card vocab-study">
            <div>
              <p className="label">Flashcards</p>
              <p className="vocab-due">{data.dueCount === 1 ? "1 word" : `${data.dueCount} words`} due for review</p>
              <p className="vocab-note">You have {data.total} saved {data.total === 1 ? "word" : "words"}.</p>
            </div>
            {data.dueCount > 0 ? (
              <Link to="/vocabulary/review" className="btn btn-primary">Study flashcards</Link>
            ) : (
              <button type="button" className="btn btn-primary" disabled>Study flashcards</button>
            )}
          </section>

          <section className="card vocab-add">
            <h2>Add a word</h2>
            <p className="vocab-add-note">Type the word and its meaning is filled in for you.</p>
            <WordForm submitLabel="Save word" onSubmit={handleAdd} />
          </section>

          {actionError && <div className="error-msg" role="alert">{actionError}</div>}

          {data.total === 0 ? (
            <div className="card empty-state"><p>You have not saved any words yet.</p></div>
          ) : (
            <ul className="vocab-list">
              {data.words.map((word) => (
                <li key={word.id} className="card vocab-item">
                  {editingId === word.id ? (
                    <WordForm
                      initial={word}
                      submitLabel="Save changes"
                      onSubmit={(fields) => handleEdit(word.id, fields)}
                      onCancel={() => setEditingId(null)}
                    />
                  ) : (
                    <>
                      <div className="vocab-text">
                        <strong>{word.word}</strong>
                        <span>{word.meaning}</span>
                        {word.example && <em>{word.example}</em>}
                        <small>{word.due ? "Due for review" : `Box ${word.box} of 5`}</small>
                      </div>
                      <div className="vocab-actions">
                        <button type="button" className="btn btn-secondary" aria-label={`Edit ${word.word}`} onClick={() => setEditingId(word.id)}>Edit</button>
                        <button type="button" className="btn btn-secondary" aria-label={`Delete ${word.word}`} onClick={() => handleDelete(word)}>Delete</button>
                      </div>
                    </>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </>
  );
}
