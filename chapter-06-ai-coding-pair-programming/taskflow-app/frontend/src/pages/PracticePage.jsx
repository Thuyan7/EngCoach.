import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { request } from "../api/client.js";
import { startToeicTest } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const PARTS = ["Part 1", "Part 2", "Part 3", "Part 4", "Part 5", "Part 6", "Part 7"];
const PART_INFO = {
  "Part 1": "Photographs · Listening",
  "Part 2": "Question–Response · Listening",
  "Part 3": "Conversations · Listening",
  "Part 4": "Short Talks · Listening",
  "Part 5": "Incomplete Sentences · Reading",
  "Part 6": "Text Completion · Reading",
  "Part 7": "Reading Comprehension · Reading",
};

// Practice is organised by Test: each Test is a separate set of questions. In a Test you either take all of its
// Parts (full test) or tick only the Parts you want.
export default function PracticePage() {
  const { token, signOut } = useAuth();
  const navigate = useNavigate();
  const [tests, setTests] = useState({ status: "loading", items: [] });
  const [selected, setSelected] = useState({});
  const [reloadKey, setReloadKey] = useState(0);
  const [starting, setStarting] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setTests({ status: "loading", items: [] });
    request("/tests", { token }).then(items => {
      if (cancelled) return;
      // A non-array reply means the request did not reach the API (e.g. the dev proxy returned the SPA page).
      if (!Array.isArray(items)) { setTests({ status: "error", items: [] }); return; }
      const available = items;
      setTests({ status: available.length ? "ready" : "empty", items: available });
      setSelected(Object.fromEntries(available.map(test => [test.id, (test.parts || []).map(p => p.part)])));
    }).catch(err => {
      if (cancelled) return;
      if (err.status === 401) return signOut();
      setTests({ status: "error", items: [] });
    });
    return () => { cancelled = true; };
  }, [token, signOut, reloadKey]);

  async function beginTest(test, chosen, key) {
    if (!chosen.length) { setError("Choose at least one Part before starting."); return; }
    setError(""); setStarting(key);
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
      if (checked) next.add(part); else next.delete(part);
      return { ...current, [testId]: PARTS.filter(name => next.has(name)) };
    });
  }

  function setAll(test, all) {
    setSelected(current => ({ ...current, [test.id]: all ? (test.parts || []).map(p => p.part) : [] }));
  }

  return <>
    <div className="page-head"><h1>Tests</h1><p>Choose a TOEIC test, then take all 7 Parts or pick only the Parts you want.</p></div>
    {error && <div className="error-msg" role="alert">{error}</div>}
    {tests.status === "loading" && <p role="status">Loading available Tests…</p>}
    {tests.status === "error" && <div><div className="error-msg" role="alert">We couldn't load the Tests.</div><button type="button" className="btn btn-secondary" onClick={() => setReloadKey(k => k + 1)}>Try again</button></div>}
    {tests.status === "empty" && <p className="card empty-state">No TOEIC Tests are published yet.</p>}
    {tests.status === "ready" && <div className="test-catalog">{tests.items.map(test => {
      const allParts = (test.parts || []).map(p => p.part);
      const chosen = selected[test.id] || [];
      const total = (test.parts || []).filter(p => chosen.includes(p.part)).reduce((sum, p) => sum + (p.questionCount || 0), 0);
      const seconds = (test.parts || []).filter(p => chosen.includes(p.part)).reduce((sum, p) => sum + (p.durationSeconds || 0), 0);
      const busy = starting !== null;
      return <article className="card test-choice" key={test.id} id={`test-${test.id}`}>
        <div className="test-choice-head">
          <div><h3>{test.title}</h3>{test.description && <p>{test.description}</p>}</div>
          <button type="button" className="btn btn-primary" disabled={busy || allParts.length === 0}
            onClick={() => beginTest(test, allParts, `full-${test.id}`)}>
            {starting === `full-${test.id}` ? "Starting…" : `Start full test (${allParts.length} Parts)`}
          </button>
        </div>
        <div className="section-head"><strong>Or choose your Parts</strong>
          <span>
            <button type="button" className="btn btn-secondary" onClick={() => setAll(test, true)}>Select all</button>{" "}
            <button type="button" className="btn btn-secondary" onClick={() => setAll(test, false)}>Clear</button>
          </span>
        </div>
        <div className="part-select-grid">{allParts.map(part => {
          const detail = test.parts.find(p => p.part === part);
          return <label key={part} className={`part-select-option${chosen.includes(part) ? " is-selected" : ""}`}>
            <input type="checkbox" checked={chosen.includes(part)} onChange={event => togglePart(test.id, part, event.target.checked)} disabled={busy} />
            <span><strong>{part}</strong><small>{PART_INFO[part] || ""}</small><small>{detail?.questionCount ?? 0} questions</small></span>
          </label>;
        })}</div>
        <p className="muted-label" aria-live="polite">{chosen.length} Parts · {total} questions · {Math.ceil(seconds / 60)} minutes</p>
        <button type="button" className="btn btn-secondary" disabled={busy || chosen.length === 0} onClick={() => beginTest(test, chosen, `test-${test.id}`)}>
          {starting === `test-${test.id}` ? "Starting…" : "Start selected Parts"}
        </button>
      </article>;
    })}</div>}
  </>;
}
