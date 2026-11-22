import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { addWord, deleteWord, flagWord, listWords, updateWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";
import WordForm from "../components/WordForm.jsx";

// The groups a learner can filter by, and study as flashcards. count() reads the totals of the list response.
const GROUPS = [
  { id: "all", label: "All", count: (data) => data.total },
  { id: "learning", label: "Learning", count: (data) => data.learningCount },
  { id: "learned", label: "Learned", count: (data) => data.learnedCount },
  { id: "marked", label: "Starred", count: (data) => data.markedCount },
];

function inGroup(word, group) {
  if (group === "learning") return !word.learned;
  if (group === "learned") return word.learned;
  if (group === "marked") return word.marked;
  return true;
}

// Vocabulary notebook: save words, star them for flashcards, sort them into learned and still learning, edit or delete
// them, and study any group as flashcards whenever you like.
export default function VocabularyPage() {
  const { token, signOut } = useAuth();
  const [data, setData] = useState(null);
  const [status, setStatus] = useState("loading");
  const [editingId, setEditingId] = useState(null);
  const [actionError, setActionError] = useState("");
  const [group, setGroup] = useState("all");

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

  // Star a word or switch it between learned and still learning, then refresh the counts.
  async function handleFlag(word, flags) {
    setActionError("");
    try {
      await flagWord(token, word.id, flags);
      await load();
    } catch (error) {
      if (error.status === 401) {
        signOut();
        return;
      }
      setActionError("We couldn't update the word. Please try again.");
    }
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

  const studyCount = data ? GROUPS.find((item) => item.id === group).count(data) : 0;
  const shown = data ? data.words.filter((word) => inGroup(word, group)) : [];

  return (
    <>
      <div className="page-head">
        <h1>Vocabulary</h1>
        <p>Save words you want to remember, then study them as flashcards whenever you like. Star the words to practise and mark the ones you already know.</p>
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
              <p className="vocab-due">{studyCount === 1 ? "1 word" : `${studyCount} words`} to study</p>
              <p className="vocab-note">Pick a group, then study it any time. You have {data.total} saved {data.total === 1 ? "word" : "words"}.</p>
            </div>
            {studyCount > 0 ? (
              <Link to={`/vocabulary/review${group === "all" ? "" : `?scope=${group}`}`} className="btn btn-primary">Study flashcards</Link>
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

          {data.total > 0 && (
            <div className="vocab-filters" role="group" aria-label="Show words">
              {GROUPS.map((item) => (
                <button key={item.id} type="button" className={`vocab-filter${group === item.id ? " is-active" : ""}`}
                  aria-pressed={group === item.id} onClick={() => setGroup(item.id)}>
                  {item.label} <span>{item.count(data)}</span>
                </button>
              ))}
            </div>
          )}

          {data.total === 0 ? (
            <div className="card empty-state"><p>You have not saved any words yet.</p></div>
          ) : shown.length === 0 ? (
            <div className="card empty-state"><p>No words in this group yet.</p></div>
          ) : (
            <ul className="vocab-list">
              {shown.map((word) => (
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
                      <button type="button" className={`vocab-star${word.marked ? " is-on" : ""}`} aria-pressed={word.marked}
                        aria-label={word.marked ? `Remove star from ${word.word}` : `Star ${word.word} for flashcards`}
                        onClick={() => handleFlag(word, { marked: !word.marked })}>{word.marked ? "★" : "☆"}</button>
                      <div className="vocab-text">
                        <strong>{word.word}</strong>
                        <span>{word.meaning}</span>
                        {word.example && <em>{word.example}</em>}
                        <small className={word.learned ? "vocab-state is-learned" : "vocab-state"}>{word.learned ? "Learned" : "Still learning"}</small>
                      </div>
                      <div className="vocab-actions">
                        <button type="button" className="btn btn-secondary" aria-label={word.learned ? `Mark ${word.word} as still learning` : `Mark ${word.word} as learned`}
                          onClick={() => handleFlag(word, { learned: !word.learned })}>{word.learned ? "Still learning" : "I know it"}</button>
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
