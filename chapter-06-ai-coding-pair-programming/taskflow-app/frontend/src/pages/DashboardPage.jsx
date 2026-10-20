import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { startPartPractice } from "../api/attempts.js";
import { listParts } from "../api/parts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const LOAD_FAILED = "We couldn't load the Part list. Please try again.";
const NO_QUESTIONS = "There are no questions for this Part yet.";
const START_FAILED = "We couldn't start this practice. Please try again.";

// S6b. Real data only — nothing here is mock data.
//  - Part list: GET /parts (S5a).
//  - Stats (average score, attempt count): NO API exists yet. The only
//    source would be GET /attempts (S10a), which is not built and whose
//    response shape is not in openapi.yaml. They are shown as "not available
//    yet" rather than as 0, because 0 would be a false statement for a user
//    who may already have attempts. See SLICE-S6B-NOTES.md.
export default function DashboardPage() {
  const { token, signOut } = useAuth();
  const navigate = useNavigate();
  const [startingPart, setStartingPart] = useState(null);
  const [startError, setStartError] = useState(null);
  const [parts, setParts] = useState({ status: "loading", items: [] });
  const [reloadKey, setReloadKey] = useState(0);

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

  async function start(part) {
    setStartError(null);
    setStartingPart(part);
    try {
      // 201 either way: a new attempt, or the unfinished one for this Part.
      const attempt = await startPartPractice(token, part);
      navigate(`/practice/${attempt.id}`);
    } catch (err) {
      if (err.status === 401) return signOut();
      setStartError(err.status === 422 ? NO_QUESTIONS : START_FAILED);
      setStartingPart(null);
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>Welcome back</h1>
        <p>Ready for today's practice?</p>
      </div>

      <section aria-labelledby="stats-heading">
        <h2 id="stats-heading" className="visually-hidden">
          Your progress
        </h2>
        <div className="stats-grid">
          <div className="stat">
            <p className="label">Average score</p>
            <p className="value">—</p>
          </div>
          <div className="stat">
            <p className="label">Attempts</p>
            <p className="value">—</p>
          </div>
        </div>
        <p className="stats-note">
          Your average score and attempt count will appear here once attempt history is available.
        </p>
      </section>

      <section aria-labelledby="parts-heading">
        <div className="section-head">
          <h2 id="parts-heading">Practice by Part</h2>
        </div>

        {parts.status === "loading" && (
          <div role="status" aria-live="polite">
            <span className="visually-hidden">Loading Parts…</span>
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
            <p>No practice exercises are available yet. Please check back soon.</p>
          </div>
        )}

        {startError && (
          <div className="error-msg" role="alert">
            {startError}
          </div>
        )}

        {parts.status === "ready" && (
          <ul className="part-grid part-list">
            {parts.items.map((name) => (
              <li key={name}>
                <button
                  type="button"
                  className="part-card"
                  disabled={startingPart !== null}
                  aria-label={`Start ${name}`}
                  onClick={() => start(name)}
                >
                  <span className="pc-name">{name}</span>
                  <span className="pc-meta">{startingPart === name ? "Starting…" : "Start practice"}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
