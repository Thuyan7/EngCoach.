import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getReviewDeck, reviewWord } from "../api/vocab.js";
import { useAuth } from "../auth/AuthContext.jsx";

// Keys that must keep their normal meaning while the learner is typing or pressing a button.
function isTyping(target) {
  return target instanceof HTMLElement && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName));
}

// Flashcard session: the words due now, one card at a time. Flip the card to see the meaning, then say whether you
// remembered it; the server schedules the next review. Space flips, the arrow keys move between cards,
// 1 = still learning and 2 = remembered.
export default function FlashcardsPage() {
  const { token, signOut } = useAuth();
  const [deck, setDeck] = useState(null); // { dueCount, cards }
  const [status, setStatus] = useState("loading");
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [results, setResults] = useState({}); // cardId -> true (remembered) | false (still learning)
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  const loadDeck = useCallback(async () => {
    setStatus("loading");
    setIndex(0);
    setRevealed(false);
    setResults({});
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

  const total = deck ? deck.cards.length : 0;
  const card = deck && index < total ? deck.cards[index] : null;
  const rated = card ? results[card.id] !== undefined : false;

  function goTo(next) {
    setIndex(Math.min(Math.max(next, 0), total));
    setRevealed(false);
    setSaveError("");
  }

  async function answer(didRemember) {
    if (!card || rated || saving) return;
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
    setResults((current) => ({ ...current, [card.id]: didRemember }));
    setSaving(false);
    goTo(index + 1);
  }

  // Keyboard shortcuts. Space and Enter are left alone on buttons and links, where they already click.
  useEffect(() => {
    if (!card) return undefined;
    function onKeyDown(event) {
      if (event.ctrlKey || event.metaKey || event.altKey || isTyping(event.target)) return;
      const onControl = event.target instanceof HTMLElement && ["BUTTON", "A"].includes(event.target.tagName);
      if (event.key === " " && !onControl) {
        event.preventDefault();
        setRevealed((value) => !value);
      } else if (event.key === "ArrowRight") {
        event.preventDefault();
        goTo(index + 1);
      } else if (event.key === "ArrowLeft") {
        event.preventDefault();
        goTo(index - 1);
      } else if (revealed && event.key === "1") {
        answer(false);
      } else if (revealed && event.key === "2") {
        answer(true);
      }
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  });

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

  if (index >= total) {
    const values = Object.values(results);
    const remembered = values.filter(Boolean).length;
    const learning = values.length - remembered;
    const skipped = total - values.length;
    const more = deck.dueCount - total;
    return (
      <div className="card flashcard-summary">
        <h1>Session complete</h1>
        <p>You remembered {remembered} and are still learning {learning}.</p>
        <div className="summary-tiles" aria-hidden="true">
          <span className="summary-tile good"><strong>{remembered}</strong>Remembered</span>
          <span className="summary-tile warn"><strong>{learning}</strong>Still learning</span>
        </div>
        {skipped > 0 && <p className="muted-label">You skipped {skipped} {skipped === 1 ? "card" : "cards"}; they stay due.</p>}
        {more > 0 && <p>{more} more {more === 1 ? "word is" : "words are"} due.</p>}
        <div className="q-actions" style={{ justifyContent: "center" }}>
          {more > 0 && <button type="button" className="btn btn-primary" onClick={loadDeck}>Study more</button>}
          <Link to="/vocabulary" className="btn btn-secondary">Back to vocabulary</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="study">
      <div className="study-top">
        <Link to="/vocabulary" className="study-exit" aria-label="End session">End session</Link>
        <p className="study-count">Card {index + 1} of {total}</p>
      </div>
      <div className="study-progress" role="progressbar" aria-label="Session progress" aria-valuemin={0} aria-valuemax={total} aria-valuenow={index}>
        <span style={{ width: `${(index / total) * 100}%` }} />
      </div>

      <button type="button" className={`flashcard${revealed ? " is-back" : ""}`} onClick={() => setRevealed(!revealed)}
        aria-label={revealed ? `${card.word}, ${card.meaning}. Show word` : `${card.word}. Show meaning`}>
        <span className="flashcard-face" key={revealed ? "back" : "front"}>
          {revealed ? (
            <>
              <span className="flashcard-side">Meaning</span>
              <span className="flashcard-meaning">{card.meaning}</span>
              {card.example && <span className="flashcard-example">{card.example}</span>}
            </>
          ) : (
            <>
              <span className="flashcard-side">Word</span>
              <span className="flashcard-word">{card.word}</span>
            </>
          )}
        </span>
        <span className="flashcard-hint">{revealed ? "Click or press Space to see the word" : "Click or press Space to flip"}</span>
      </button>

      <div className="study-controls">
        <button type="button" className="btn btn-secondary study-arrow" aria-label="Previous card" disabled={index === 0} onClick={() => goTo(index - 1)}>←</button>
        {rated ? (
          <p className="study-rated" role="status">{results[card.id] ? "You remembered this word" : "You marked this word as still learning"}</p>
        ) : (
          <div className="study-rate">
            <button type="button" className="btn btn-secondary rate-learning" aria-label="Still learning" disabled={saving || !revealed} onClick={() => answer(false)}>
              Still learning <kbd>1</kbd>
            </button>
            <button type="button" className="btn btn-primary rate-remembered" aria-label="I remembered" disabled={saving || !revealed} onClick={() => answer(true)}>
              I remembered <kbd>2</kbd>
            </button>
          </div>
        )}
        <button type="button" className="btn btn-secondary study-arrow" aria-label="Next card" onClick={() => goTo(index + 1)}>→</button>
      </div>
      {saveError && <div className="error-msg" role="alert">{saveError}</div>}
      <p className="study-keys">Keys: <kbd>Space</kbd> flip · <kbd>←</kbd> <kbd>→</kbd> change card · <kbd>1</kbd> still learning · <kbd>2</kbd> remembered</p>
    </div>
  );
}
