import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listAttempts, startPartPractice } from "../api/attempts.js";
import { listParts } from "../api/parts.js";
import { useAuth } from "../auth/AuthContext.jsx";
import { summarizeAttempts } from "../stats.js";

const LOAD_FAILED = "We couldn't load the Part list. Please try again.";
const NO_QUESTIONS = "There are no questions for this Part yet.";
const START_FAILED = "We couldn't start this practice. Please try again.";
const STATS_FAILED = "We couldn't load your stats. Please try again.";

export default function DashboardPage() {
  const { token, signOut } = useAuth();
  const navigate = useNavigate();
  const [startingPart, setStartingPart] = useState(null);
  const [startError, setStartError] = useState(null);
  const [parts, setParts] = useState({ status: "loading", items: [] });
  const [reloadKey, setReloadKey] = useState(0);
  const [stats, setStats] = useState({ status: "loading", attempts: [] });
  const [statsReloadKey, setStatsReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setParts({ status: "loading", items: [] });
    listParts(token)
      .then((items) => {
        if (cancelled) return;
        setParts({ status: items.length === 0 ? "empty" : "ready", items });
      })
      .catch((err) => {
        if (cancelled) return;
        if (err.status === 401) { signOut(); return; }
        setParts({ status: "error", items: [] });
      });
    return () => { cancelled = true; };
  }, [token, signOut, reloadKey]);

  useEffect(() => {
    let cancelled = false;
    setStats({ status: "loading", attempts: [] });
    listAttempts(token)
      .then((attempts) => { if (!cancelled) setStats({ status: "ready", attempts }); })
      .catch((err) => {
        if (cancelled) return;
        if (err.status === 401) { signOut(); return; }
        setStats({ status: "error", attempts: [] });
      });
    return () => { cancelled = true; };
  }, [token, signOut, statsReloadKey]);

  async function start(part) {
    setStartError(null);
    setStartingPart(part);
    try {
      const attempt = await startPartPractice(token, part);
      navigate(`/practice/${attempt.id}`);
    } catch (err) {
      if (err.status === 401) return signOut();
      setStartError(err.status === 422 ? NO_QUESTIONS : START_FAILED);
      setStartingPart(null);
    }
  }

  const summary = summarizeAttempts(stats.attempts);
  const average = stats.status === "ready" && summary.averagePercent !== null;
  const averageLabel = stats.status === "loading" ? "…" : average ? `${summary.averagePercent}%` : "—";
  const countLabel = stats.status === "loading" ? "…" : stats.status === "ready" ? summary.attemptCount : "—";

  return (
    <>
      <section className="dashboard-welcome">
        <div className="dashboard-welcome-copy">
          <p className="eyebrow">YOUR LEARNING OVERVIEW</p>
          <h1>Welcome back<span className="welcome-period">.</span></h1>
          <p>Build your TOEIC confidence with focused practice, clear feedback, and steady progress.</p>
        </div>
        <div className="welcome-art" aria-hidden="true">
          <div className="welcome-art-orbit orbit-one" />
          <div className="welcome-art-orbit orbit-two" />
          <div className="welcome-book">
            <svg viewBox="0 0 96 96" fill="none">
              <path d="M15 22c13-5 24-3 33 4v48c-9-7-20-9-33-4V22Z" fill="#fff" stroke="#B9D7F2" strokeWidth="2.5"/>
              <path d="M81 22c-13-5-24-3-33 4v48c9-7 20-9 33-4V22Z" fill="#EAF4FF" stroke="#B9D7F2" strokeWidth="2.5"/>
              <path d="M25 35c7-2 12-1 17 2M25 44c7-2 12-1 17 2M71 35c-7-2-12-1-17 2M71 44c-7-2-12-1-17 2" stroke="#4C86BC" strokeWidth="2" strokeLinecap="round"/>
              <path d="m48 10 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z" fill="#F6C96B"/>
            </svg>
          </div>
          <span className="welcome-spark spark-one">✦</span>
          <span className="welcome-spark spark-two">✧</span>
        </div>
      </section>

      <section aria-labelledby="stats-heading" className="dashboard-stats" aria-busy={stats.status === "loading"}>
        <h2 id="stats-heading" className="visually-hidden">Your progress</h2>
        <div className="stat stat-average">
          <span className="stat-icon" aria-hidden="true">↗</span>
          <div><p className="label">Average score</p><p className="value">{averageLabel}</p></div>
          <span className="stat-caption">Across completed tests</span>
        </div>
        <div className="stat stat-attempts">
          <span className="stat-icon" aria-hidden="true">✓</span>
          <div><p className="label">Completed attempts</p><p className="value">{countLabel}</p></div>
          <span className="stat-caption">Every session counts</span>
        </div>
        {stats.status === "ready" && summary.averagePercent === null && <p className="stats-note">Complete a practice to see your average score.</p>}
        {stats.status === "ready" && summary.averagePercent !== null && <p className="stats-note">Average percentage correct across your completed attempts.</p>}
        {stats.status === "error" && <div className="stats-error"><div className="error-msg" role="alert">{STATS_FAILED}</div><button type="button" className="btn btn-secondary" onClick={() => setStatsReloadKey((k) => k + 1)}>Try again</button></div>}
      </section>

      <section aria-labelledby="parts-heading" className="dashboard-practice-section">
        <div className="section-head">
          <div><p className="eyebrow">CHOOSE YOUR FOCUS</p><h2 id="parts-heading">Practice by Part</h2></div>
          <span className="section-helper">Pick a section to get started <span aria-hidden="true">→</span></span>
        </div>
        {parts.status === "loading" && <div role="status" aria-live="polite"><span className="visually-hidden">Loading Parts…</span><div className="part-grid" aria-hidden="true">{[0,1,2,3].map((i) => <div key={i} className="skeleton part-skeleton" />)}</div></div>}
        {parts.status === "error" && <div><div className="error-msg" role="alert">{LOAD_FAILED}</div><button type="button" className="btn btn-secondary" onClick={() => setReloadKey((k) => k + 1)}>Try again</button></div>}
        {parts.status === "empty" && <div className="card empty-state"><p>No practice exercises are available yet. Please check back soon.</p></div>}
        {startError && <div className="error-msg" role="alert">{startError}</div>}
        {parts.status === "ready" && <ul className="part-grid part-list">{parts.items.map((name, index) => (
          <li key={name}>
            <button type="button" className="part-card" disabled={startingPart !== null} aria-label={`Start ${name}`} onClick={() => start(name)}>
              <span className="part-card-top"><span className="part-number">{String(index + 1).padStart(2, "0")}</span><span className="part-card-arrow" aria-hidden="true">↗</span></span>
              <span className="pc-name">{name}</span>
              <span className="pc-meta">{startingPart === name ? "Starting…" : "Practice at your own pace"}</span>
            </button>
          </li>
        ))}</ul>}
      </section>
    </>
  );
}
