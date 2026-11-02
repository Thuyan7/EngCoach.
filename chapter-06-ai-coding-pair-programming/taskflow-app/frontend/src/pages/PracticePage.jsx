import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listParts } from "../api/parts.js";
import { request } from "../api/client.js";
import { startMockTest, startPartPractice, startToeicTest } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const PARTS = ["Part 1", "Part 2", "Part 3", "Part 4", "Part 5", "Part 6", "Part 7"];

export default function PracticePage() {
  const { token, signOut } = useAuth();
  const navigate = useNavigate();
  const [parts, setParts] = useState({ status: "loading", items: [] });
  const [tests, setTests] = useState({ status: "loading", items: [] });
  const [selected, setSelected] = useState({});
  const [reloadKey, setReloadKey] = useState(0);
  const [starting, setStarting] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    listParts(token).then(items => { if (!cancelled) setParts({ status: items.length ? "ready" : "empty", items }); })
      .catch(err => { if (cancelled) return; if (err.status === 401) return signOut(); setParts({ status: "error", items: [] }); });
    request("/tests", { token }).then(items => {
      if (!cancelled) {
        const available = Array.isArray(items) ? items : [];
        setTests({ status: available.length ? "ready" : "empty", items: available });
        setSelected(current => Object.fromEntries(available.map(test => [test.id, current[test.id] || (test.parts || []).map(p => p.part)])));
      }
    }).catch(err => { if (cancelled) return; if (err.status === 401) return signOut(); setTests({ status: "error", items: [] }); });
    return () => { cancelled = true; };
  }, [token, signOut, reloadKey]);

  async function begin(type, part = null) {
    setError(""); setStarting(type === "MOCK_TEST" ? "mock" : part);
    try {
      const attempt = type === "MOCK_TEST" ? await startMockTest(token) : await startPartPractice(token, part);
      navigate(`/practice/${attempt.id}`);
    } catch (err) {
      if (err.status === 401) return signOut();
      setError(err.status === 422 ? "There are no questions available for this practice yet." : "We couldn't start this practice. Please try again.");
      setStarting(null);
    }
  }

  async function beginTest(test) {
    const chosen = selected[test.id] || [];
    if (!chosen.length) { setError("Choose at least one Part before starting."); return; }
    setError(""); setStarting(`test-${test.id}`);
    try {
      const attempt = await startToeicTest(token, test.id, chosen);
      navigate(`/practice/${attempt.id}`);
    } catch (err) {
      if (err.status === 401) return signOut();
      setError(err.status === 422 ? "The selected Parts do not have enough available questions." : "We couldn't start this Test. Please try again.");
      setStarting(null);
    }
  }

  function togglePart(testId, part, checked) {
    setSelected(current => {
      const next = new Set(current[testId] || []);
      checked ? next.add(part) : next.delete(part);
      return { ...current, [testId]: PARTS.filter(name => next.has(name)) };
    });
  }

  function selectAll(test) {
    const available = (test.parts || []).map(p => p.part);
    setSelected(current => ({ ...current, [test.id]: available }));
  }

  return <>
    <div className="page-head"><h1>Practice</h1><p>Choose a TOEIC test and select the Parts you want to practice.</p></div>
    {error && <div className="error-msg" role="alert">{error}</div>}
    <section aria-labelledby="test-catalog-heading">
      <div className="section-head"><h2 id="test-catalog-heading">TOEIC Test catalog</h2><span className="muted-label">Choose one or more Parts</span></div>
      {tests.status === "loading" && <p role="status">Loading available Tests…</p>}
      {tests.status === "error" && <div><p className="error-msg" role="alert">We couldn't load the Test catalog.</p><button type="button" className="btn btn-secondary" onClick={() => setReloadKey(k => k + 1)}>Try again</button></div>}
      {tests.status === "empty" && <p className="card empty-state">No TOEIC Tests are published yet.</p>}
      {tests.status === "ready" && <div className="test-catalog">{tests.items.map(test => {
        const availableParts = (test.parts || []).map(p => p.part);
        const chosen = selected[test.id] || [];
        const total = (test.parts || []).filter(p => chosen.includes(p.part)).reduce((sum, p) => sum + (p.questionCount || 0), 0);
        const seconds = (test.parts || []).filter(p => chosen.includes(p.part)).reduce((sum, p) => sum + (p.durationSeconds || 0), 0);
        return <article className="card test-choice" key={test.id}>
          <h3>{test.title}</h3>
          {test.description && <p>{test.description}</p>}
          <div className="section-head"><strong>Select Parts</strong><button type="button" className="btn btn-secondary" onClick={() => selectAll(test)}>Select all</button></div>
          <div className="part-select-grid">{availableParts.map(part => {
            const detail = test.parts.find(p => p.part === part);
            return <label key={part} className="part-select-option"><input type="checkbox" checked={chosen.includes(part)} onChange={event => togglePart(test.id, part, event.target.checked)} disabled={starting !== null} /><span>{part}</span><small>{detail?.questionCount ?? 0} questions</small></label>;
          })}</div>
          <p className="muted-label" aria-live="polite">{chosen.length} Parts · {total} questions · {Math.ceil(seconds / 60)} minutes</p>
          <button type="button" className="btn btn-primary" disabled={starting !== null || chosen.length === 0} onClick={() => beginTest(test)}>{starting === `test-${test.id}` ? "Starting…" : "Start selected Parts"}</button>
        </article>;
      })}</div>}
    </section>
    <section aria-labelledby="mock-test-heading" className="practice-feature">
      <div className="practice-feature-copy"><p className="eyebrow">Legacy timed assessment</p><h2 id="mock-test-heading">Full TOEIC mock test</h2><p>Start the existing full two-hour mock-test flow.</p></div>
      <button type="button" className="btn btn-primary" disabled={starting !== null} onClick={() => begin("MOCK_TEST")}>{starting === "mock" ? "Starting…" : "Start mock test"}</button>
    </section>
    <section aria-labelledby="part-practice-heading"><div className="section-head"><h2 id="part-practice-heading">Practice by Part</h2><span className="muted-label">Untimed</span></div>
      {parts.status === "loading" && <p role="status">Loading available Parts…</p>}
      {parts.status === "error" && <div><div className="error-msg" role="alert">We couldn't load the Part list. Please try again.</div><button type="button" className="btn btn-secondary" onClick={() => setReloadKey(k => k + 1)}>Try again</button></div>}
      {parts.status === "empty" && <div className="card empty-state"><p>No Part practice is available yet.</p></div>}
      {parts.status === "ready" && <ul className="part-grid part-list">{parts.items.map(part => <li key={part}><button type="button" className="part-card" disabled={starting !== null} onClick={() => begin("PART_PRACTICE", part)}><span className="pc-name">{part}</span><span className="pc-meta">{starting === part ? "Starting…" : "Practice at your own pace"}</span></button></li>)}</ul>}
    </section>
  </>;
}
