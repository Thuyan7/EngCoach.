import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getReviewDeck, reviewWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";

// Flashcard session: shows the words due now one at a time. The learner reveals the meaning and says
// whether they remembered it; the server then schedules the next review.
export default function FlashcardsPage() {
  const { token, signOut } = useAuth();
  const [deck, setDeck] = useState(null); // { dueCount, cards }
  const [status, setStatus] = useState("loading");
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [remembered, setRemembered] = useState(0);
  const [learning, setLearning] = useState(0);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  const loadDeck = useCallback(async () => {
    setStatus("loading");
    setIndex(0);
    setRevealed(false);
    setRemembered(0);
    setLearning(0);
    setSaveError("");
    try {
      setDeck(await getReviewDeck(token));
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
    loadDeck();
  }, [loadDeck]);

  async function answer(didRemember) {
    const card = deck.cards[index];
    setSaving(true);
    setSaveError("");
    try {
      await reviewWord(token, card.id, didRemember);
    } catch (error) {
      if (error.status === 401) {
        signOut();
        return;
      }
      setSaveError("We couldn't save your answer. Please try again.");
      setSaving(false);
      return;
    }
    if (didRemember) setRemembered(remembered + 1);
    else setLearning(learning + 1);
    setIndex(index + 1);
    setRevealed(false);
    setSaving(false);
  }

  if (status === "loading") return <p role="status">Loading your flashcards…</p>;

  if (status === "error") {
    return (
      <div>
        <div className="error-msg" role="alert">We couldn't load your flashcards. Please try again.</div>
        <button type="button" className="btn btn-secondary" onClick={loadDeck}>Try again</button>
      </div>
    );
  }

  if (deck.cards.length === 0) {
    return (
      <div className="card empty-state">
        <h1>Flashcards</h1>
        <p>No words are due for review right now.</p>
        <div className="q-actions" style={{ justifyContent: "center" }}>
          <Link to="/vocabulary" className="btn btn-primary">Back to vocabulary</Link>
        </div>
      </div>
    );
  }

  if (index >= deck.cards.length) {
    const more = deck.dueCount - deck.cards.length;
    return (
      <div className="card flashcard-summary">
        <h1>Session complete</h1>
        <p>You remembered {remembered} and are still learning {learning}.</p>
        {more > 0 && <p>{more} more {more === 1 ? "word is" : "words are"} due.</p>}
        <div className="q-actions" style={{ justifyContent: "center" }}>
          {more > 0 && <button type="button" className="btn btn-primary" onClick={loadDeck}>Study more</button>}
          <Link to="/vocabulary" className="btn btn-secondary">Back to vocabulary</Link>
        </div>
      </div>
    );
  }

  const card = deck.cards[index];
  return (
    <>
      <div className="page-head">
        <h1>Flashcards</h1>
        <p>Card {index + 1} of {deck.cards.length}</p>
      </div>

      <section className="card flashcard" aria-label="Flashcard">
        <p className="flashcard-word">{card.word}</p>
        {revealed ? (
          <div className="flashcard-back">
            <p className="flashcard-meaning">{card.meaning}</p>
            {card.example && <p className="flashcard-example">{card.example}</p>}
          </div>
        ) : (
          <button type="button" className="btn btn-secondary" onClick={() => setRevealed(true)}>Show meaning</button>
        )}
      </section>

      {revealed && (
        <div className="q-actions" style={{ justifyContent: "center" }}>
          <button type="button" className="btn btn-secondary" disabled={saving} onClick={() => answer(false)}>Still learning</button>
          <button type="button" className="btn btn-primary" disabled={saving} onClick={() => answer(true)}>I remembered</button>
        </div>
      )}
      {saveError && <div className="error-msg" role="alert">{saveError}</div>}
      <p className="flashcard-exit"><Link to="/vocabulary">End session</Link></p>
    </>
  );
}
