import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { listAttempts, startToeicTest } from "../api/attempts.js";
import { request } from "../api/client.js";
import { useAuth } from "../auth/AuthContext.jsx";
import { summarizeAttempts } from "../stats.js";

const LOAD_FAILED = "We couldn't load the Tests. Please try again.";
const NO_QUESTIONS = "There are no questions for this Test yet.";
const START_FAILED = "We couldn't start this practice. Please try again.";
const STATS_FAILED = "We couldn't load your stats. Please try again.";

// S6b. Real data only — nothing here is mock data.
//  - Test list: GET /tests. Practice is organised by Test; each card starts the full Test or opens the Parts chooser.
//  - Stats: GET /attempts (S10a) — completed attempts. The Part list and the
//    stats load independently, so a failure in one never hides the other.
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
    request("/tests", { token })
      .then((data) => {
        if (cancelled) return;
        if (!Array.isArray(data)) {
          setParts({ status: "error", items: [] });
          return;
        }
        const items = data;
        setParts({ status: items.length === 0 ? "empty" : "ready", items });
      })
      .catch((err) => {
        if (cancelled) return;
        // Expired/invalid JWT: end the session; ProtectedRoute sends the
        // user to Login.
        if (err.status === 401) {
          signOut();
          return;
        }
        setParts({ status: "error", items: [] });
      });
    return () => {
      cancelled = true;
    };
  }, [token, signOut, reloadKey]);

  useEffect(() => {
    let cancelled = false;
    setStats({ status: "loading", attempts: [] });
    listAttempts(token)
      .then((attempts) => {
        if (!cancelled) setStats({ status: "ready", attempts });
      })
      .catch((err) => {
        if (cancelled) return;
        if (err.status === 401) {
          signOut();
          return;
        }
        setStats({ status: "error", attempts: [] });
      });
    return () => {
      cancelled = true;
    };
  }, [token, signOut, statsReloadKey]);

  async function start(test) {
    setStartError(null);
    setStartingPart(test.id);
    try {
      // 201 either way: a new attempt, or the unfinished one for this Test and Parts.
      const attempt = await startToeicTest(token, test.id, (test.parts || []).map((p) => p.part));
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
      <div className="page-head">
        <h1>Welcome back</h1>
        <p>Ready for today's practice?</p>
      </div>

      <section aria-labelledby="stats-heading" aria-busy={stats.status === "loading"}>
        <h2 id="stats-heading" className="visually-hidden">
          Your progress
        </h2>
        <div className="stats-grid">
          <div className="stat">
            <p className="label">Average score</p>
            <p className="value">{averageLabel}</p>
          </div>
          <div className="stat">
            <p className="label">Attempts completed</p>
            <p className="value">{countLabel}</p>
          </div>
        </div>
        {stats.status === "ready" && summary.averagePercent === null && (
          <p className="stats-note">Complete a practice to see your average score.</p>
        )}
        {stats.status === "ready" && summary.averagePercent !== null && (
          <p className="stats-note">Average of the percentage correct across your completed attempts.</p>
        )}
        {stats.status === "error" && (
          <div>
            <div className="error-msg" role="alert">
              {STATS_FAILED}
            </div>
            <button type="button" className="btn btn-secondary" onClick={() => setStatsReloadKey((k) => k + 1)}>
              Try again
            </button>
          </div>
        )}
      </section>

      <section aria-labelledby="parts-heading">
        <div className="section-head">
          <h2 id="parts-heading">Tests</h2>
          <Link to="/practice" className="muted-label">See all Tests</Link>
        </div>

        {parts.status === "loading" && (
          <div role="status" aria-live="polite">
            <span className="visually-hidden">Loading Tests…</span>
            <div className="part-grid" aria-hidden="true">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="skeleton part-skeleton" />
              ))}
            </div>
          </div>
        )}

        {parts.status === "error" && (
          <div>
            <div className="error-msg" role="alert">
              {LOAD_FAILED}
            </div>
            <button type="button" className="btn btn-secondary" onClick={() => setReloadKey((k) => k + 1)}>
              Try again
            </button>
          </div>
        )}

        {parts.status === "empty" && (
          <div className="card empty-state">
            <p>No Tests are available yet. Please check back soon.</p>
          </div>
        )}

        {startError && (
          <div className="error-msg" role="alert">
            {startError}
          </div>
        )}

        {parts.status === "ready" && (
          <ul className="dash-test-list">
            {parts.items.map((test) => {
              const list = test.parts || [];
              const questions = list.reduce((sum, p) => sum + (p.questionCount || 0), 0);
              return (
                <li key={test.id} className="card dash-test">
                  <div>
                    <h3>{test.title}</h3>
                    <p className="muted-label">{list.length} Parts · {questions} questions</p>
                  </div>
                  <div className="dash-test-actions">
                    <button
                      type="button"
                      className="btn btn-primary"
                      disabled={startingPart !== null || list.length === 0}
                      aria-label={`Start full ${test.title}`}
                      onClick={() => start(test)}
                    >
                      {startingPart === test.id ? "Starting…" : "Start full test"}
                    </button>
                    <Link to="/practice" className="btn btn-secondary" aria-label={`Choose Parts of ${test.title}`}>
                      Choose Parts
                    </Link>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </>
  );
}
