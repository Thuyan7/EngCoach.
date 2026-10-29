import { useMemo, useState } from "react";

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
  const [selectedParts, setSelectedParts] = useState(() => PARTS.map((part) => part.id));
  const [showPreview, setShowPreview] = useState(false);

  const selected = useMemo(
    () => PARTS.filter((part) => selectedParts.includes(part.id)),
    [selectedParts],
  );

  function togglePart(id) {
    setSelectedParts((current) =>
      current.includes(id) ? current.filter((partId) => partId !== id) : [...current, id].sort((a, b) => a - b),
    );
    setShowPreview(false);
  }

  function toggleAll() {
    setSelectedParts((current) => current.length === PARTS.length ? [] : PARTS.map((part) => part.id));
    setShowPreview(false);
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
            <span className="test-status">PRACTICE TEMPLATE</span>
            <h2 id="test-card-title">Test 01 · TOEIC Practice</h2>
            <p>A flexible practice setup covering all seven TOEIC Parts.</p>
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
            <span>{selectedParts.length === 7 ? "Full test configuration" : selectedParts.length === 0 ? "Select at least one Part to preview" : "Custom practice configuration"}</span>
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
              <span className="test-status">CONFIGURATION PREVIEW</span>
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
            This is a UI preview only. No attempt has been created, no questions have been loaded, and no answers or scores have been saved.
          </p>
        </section>
      )}
    </>
  );
}
