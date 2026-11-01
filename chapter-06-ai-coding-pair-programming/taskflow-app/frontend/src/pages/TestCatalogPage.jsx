import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { startMockTest, startPartPractice } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const PARTS = [
  { id: 1, title: "Part 1", subtitle: "Photographs", skill: "Listening" },
  { id: 2, title: "Part 2", subtitle: "Question–Response", skill: "Listening" },
  { id: 3, title: "Part 3", subtitle: "Conversations", skill: "Listening" },
  { id: 4, title: "Part 4", subtitle: "Short Talks", skill: "Listening" },
  { id: 5, title: "Part 5", subtitle: "Incomplete Sentences", skill: "Reading" },
  { id: 6, title: "Part 6", subtitle: "Text Completion", skill: "Reading" },
  { id: 7, title: "Part 7", subtitle: "Reading Comprehension", skill: "Reading" },
];

export default function TestCatalogPage() {
  const { token, signOut } = useAuth();
  const navigate = useNavigate();
  const [selectedParts, setSelectedParts] = useState(() => PARTS.map((part) => part.id));
  const [showPreview, setShowPreview] = useState(false);
  const [startState, setStartState] = useState({ status: "idle" });

  const selected = useMemo(
    () => PARTS.filter((part) => selectedParts.includes(part.id)),
    [selectedParts],
  );

  function togglePart(id) {
    setSelectedParts((current) =>
      current.includes(id) ? current.filter((partId) => partId !== id) : [...current, id].sort((a, b) => a - b),
    );
    setShowPreview(false);
    setStartState({ status: "idle" });
  }

  function toggleAll() {
    setSelectedParts((current) => current.length === PARTS.length ? [] : PARTS.map((part) => part.id));
    setShowPreview(false);
    setStartState({ status: "idle" });
  }

  async function startSelected() {
    if (selectedParts.length === 0 || startState.status === "starting") return;
    setStartState({ status: "starting" });
    try {
      const isFullTest = selectedParts.length === PARTS.length;
      const attempt = isFullTest
        ? await startMockTest(token)
        : await startPartPractice(token, selectedParts.map((id) => `Part ${id}`));
      navigate(`/practice/${attempt.id}`);
    } catch (error) {
      if (error.status === 401) {
        signOut();
        return;
      }
      setStartState({
        status: "error",
        message: error.status === 422
          ? "Some selected Parts do not have questions yet. Please choose other Parts."
          : "Could not start this practice. Please try again.",
      });
    }
  }

  return (
    <>
      <header className="page-head">
        <p className="test-eyebrow">ENGCOACH PRACTICE</p>
        <h1>Tests</h1>
        <p>Choose a test and select the TOEIC Parts you want to practice.</p>
      </header>

      <section className="test-catalog" aria-labelledby="test-card-title">
        <div className="test-hero">
          <div className="test-hero-copy">
            <span className="test-status">PRACTICE SET</span>
            <h2 id="test-card-title">Test 01 · TOEIC Practice</h2>
            <p>Choose one Part, several Parts, or the complete seven-Part set.</p>
            <div className="test-tags">
              <span>Listening · Parts 1–4</span>
              <span>Reading · Parts 5–7</span>
            </div>
          </div>
          <div className="test-hero-number" aria-hidden="true">01</div>
        </div>

        <div className="test-selection-head">
          <div>
            <h3>Select Parts</h3>
            <p>Choose one Part, several Parts, or all seven.</p>
          </div>
          <button type="button" className="text-button" onClick={toggleAll}>
            {selectedParts.length === PARTS.length ? "Clear selection" : "Select all 7"}
          </button>
        </div>

        <div className="test-part-grid">
          {PARTS.map((part) => {
            const checked = selectedParts.includes(part.id);
            return (
              <label key={part.id} className={`test-part-option${checked ? " is-selected" : ""}`}>
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => togglePart(part.id)}
                  aria-label={`${part.title}: ${part.subtitle}`}
                />
                <span className="test-part-check" aria-hidden="true">{checked ? "✓" : ""}</span>
                <span className="test-part-main">
                  <strong>{part.title}</strong>
                  <span>{part.subtitle}</span>
                  <small>{part.skill}</small>
                </span>
              </label>
            );
          })}
        </div>

        <div className="test-selection-footer">
          <div className="test-selected-count" aria-live="polite">
            <strong>{selectedParts.length} of 7 Parts selected</strong>
            <span>{selectedParts.length === 7 ? "Full test selection" : selectedParts.length === 0 ? "Select at least one Part" : "Custom Part practice"}</span>
          </div>
          <button
            type="button"
            className="btn btn-primary"
            disabled={selectedParts.length === 0}
            onClick={() => setShowPreview(true)}
          >
            Preview selection
          </button>
        </div>
      </section>

      {showPreview && (
        <section className="test-preview card" aria-labelledby="preview-heading" aria-live="polite">
          <div className="test-preview-heading">
            <div>
              <span className="test-status">READY TO START</span>
              <h2 id="preview-heading">Your selected Parts</h2>
            </div>
            <span className="test-preview-count">{selected.length}/7</span>
          </div>
          <ol className="test-preview-list">
            {selected.map((part) => (
              <li key={part.id}>
                <span><strong>{part.title}</strong> — {part.subtitle}</span>
                <small>{part.skill}</small>
              </li>
            ))}
          </ol>
          <p className="test-preview-note">
            {selected.length === 7
              ? "Starting all seven Parts uses the timed full-test flow (120 minutes)."
              : "Only the selected Parts will be included in this practice attempt."}
          </p>
          {startState.status === "error" && <p className="error-msg" role="alert">{startState.message}</p>}
          <button
            type="button"
            className="btn btn-primary"
            disabled={startState.status === "starting"}
            onClick={startSelected}
          >
            {startState.status === "starting" ? "Starting…" : selected.length === 7 ? "Start full test" : "Start selected Parts"}
          </button>
        </section>
      )}
    </>
  );
}
