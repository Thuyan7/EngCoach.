import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listParts } from "../api/parts.js";
import { startMockTest, startPartPractice } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const partDescriptions = [
  "Photographs and visual descriptions",
  "Question and response",
  "Short conversations",
  "Short talks and announcements",
  "Incomplete sentences",
  "Text completion",
  "Reading comprehension",
];

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
    listParts(token).then((items) => {
      if (!cancelled) setParts({ status: items.length ? "ready" : "empty", items });
    }).catch((err) => {
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
      setError(err.status === 422 ? "There are no questions available for this practice yet." : "We couldn't start this practice. Please try again.");
      setStarting(null);
    }
  }

  return (
    <div className="practice-page">
      <div className="page-head"><p className="eyebrow">TRAIN WITH PURPOSE</p><h1>Practice Tests</h1><p>Simulate the real exam or focus on one skill at a time.</p></div>
      {error && <div className="error-msg" role="alert">{error}</div>}
      <section aria-labelledby="mock-test-heading" className="mock-feature">
        <div className="mock-feature-copy">
          <span className="mock-feature-label">MOST POPULAR</span>
          <h2 id="mock-test-heading">Full Mock Test</h2>
          <p>All available TOEIC parts in one timed practice. Your answers are saved as you go, and you can review your result after submitting.</p>
          <div className="mock-feature-meta"><span>◷ &nbsp;120 minutes</span><span>☷ &nbsp;Full test</span><span>♫ &nbsp;Listening + Reading</span></div>
        </div>
        <button type="button" className="btn btn-green mock-start" disabled={starting !== null} onClick={() => begin("MOCK_TEST")}>{starting === "mock" ? "Starting…" : "▷  Start mock test"}</button>
      </section>
      <section aria-labelledby="part-practice-heading" className="practice-parts-section">
        <div className="home-section-heading"><div><p className="eyebrow">BUILD ONE SKILL AT A TIME</p><h2 id="part-practice-heading">Practice by Part</h2></div><span>Choose a section to get started</span></div>
        {parts.status === "loading" && <div role="status" className="part-loading">Loading available Parts…</div>}
        {parts.status === "error" && <div><div className="error-msg" role="alert">We couldn't load the Part list. Please try again.</div><button type="button" className="btn btn-secondary" onClick={() => setReloadKey((key) => key + 1)}>Try again</button></div>}
        {parts.status === "empty" && <div className="card empty-state"><p>No Part practice is available yet.</p></div>}
        {parts.status === "ready" && <ul className="practice-part-grid">{parts.items.map((part, index) => <li key={part}><button type="button" className="practice-part-card" disabled={starting !== null} onClick={() => begin("PART_PRACTICE", part)}><span className="practice-part-top"><span className="practice-part-number">P{index + 1}</span><span className="practice-part-type">{index < 4 ? "Listening" : "Reading"}</span></span><strong>{part}</strong><p>{partDescriptions[index] || "Focused TOEIC practice"}</p><span className="practice-part-bottom">{starting === part ? "Starting…" : "Practice this Part"} <b>→</b></span></button></li>)}</ul>}
      </section>
    </div>
  );
}
