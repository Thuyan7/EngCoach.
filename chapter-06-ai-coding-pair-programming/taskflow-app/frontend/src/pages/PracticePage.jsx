import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listParts } from "../api/parts.js";
import { startMockTest, startPartPractice } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

export default function PracticePage() {
  const { token, signOut } = useAuth();
  const navigate = useNavigate();
  const [parts, setParts] = useState({ status: "loading", items: [] });
  const [reloadKey, setReloadKey] = useState(0);
  const [starting, setStarting] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setParts({ status: "loading", items: [] });
    listParts(token)
      .then((items) => {
        if (!cancelled) setParts({ status: items.length ? "ready" : "empty", items });
      })
      .catch((err) => {
        if (cancelled) return;
        if (err.status === 401) return signOut();
        setParts({ status: "error", items: [] });
      });
    return () => { cancelled = true; };
  }, [token, signOut, reloadKey]);

  async function begin(type, part = null) {
    setError("");
    setStarting(type === "MOCK_TEST" ? "mock" : part);
    try {
      const attempt = type === "MOCK_TEST"
        ? await startMockTest(token)
        : await startPartPractice(token, part);
      navigate(`/practice/${attempt.id}`);
    } catch (err) {
      if (err.status === 401) return signOut();
      setError(err.status === 422
        ? "There are no questions available for this practice yet."
        : "We couldn't start this practice. Please try again.");
      setStarting(null);
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>Practice</h1>
        <p>Choose a focused TOEIC Part practice or simulate the full two-hour test.</p>
      </div>

      {error && <div className="error-msg" role="alert">{error}</div>}

      <section aria-labelledby="mock-test-heading" className="practice-feature">
        <div className="practice-feature-copy">
          <p className="eyebrow">Timed assessment</p>
          <h2 id="mock-test-heading">Full TOEIC mock test</h2>
          <p>Practice with the question bank in a timed two-hour session. Your answers are saved as you go.</p>
          <ul className="practice-feature-points">
            <li>120-minute countdown</li>
            <li>Review answers before submitting</li>
            <li>Get your score and feedback after grading</li>
          </ul>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          disabled={starting !== null}
          onClick={() => begin("MOCK_TEST")}
        >
          {starting === "mock" ? "Starting…" : "Start mock test"}
        </button>
      </section>

      <section aria-labelledby="part-practice-heading">
        <div className="section-head">
          <h2 id="part-practice-heading">Practice by Part</h2>
          <span className="muted-label">Untimed</span>
        </div>
        {parts.status === "loading" && (
          <div role="status" aria-live="polite">
            <span className="visually-hidden">Loading available Parts…</span>
            <div className="part-grid" aria-hidden="true">
              {[0, 1, 2, 3].map((n) => <div key={n} className="skeleton part-skeleton" />)}
            </div>
          </div>
        )}
        {parts.status === "error" && (
          <div>
            <div className="error-msg" role="alert">We couldn't load the Part list. Please try again.</div>
            <button type="button" className="btn btn-secondary" onClick={() => setReloadKey((k) => k + 1)}>Try again</button>
          </div>
        )}
        {parts.status === "empty" && (
          <div className="card empty-state"><p>No Part practice is available yet.</p></div>
        )}
        {parts.status === "ready" && (
          <ul className="part-grid part-list">
            {parts.items.map((part) => (
              <li key={part}>
                <button
                  type="button"
                  className="part-card"
                  disabled={starting !== null}
                  onClick={() => begin("PART_PRACTICE", part)}
                >
                  <span className="pc-name">{part}</span>
                  <span className="pc-meta">{starting === part ? "Starting…" : "Practice at your own pace"}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
