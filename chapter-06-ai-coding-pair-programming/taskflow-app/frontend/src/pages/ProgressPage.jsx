import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { deleteAttempt, listAttempts } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const formatDate = (value) => {
  if (!value) return "Date unavailable";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "Date unavailable"
    : new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(date);
};

const attemptName = (attempt) =>
  attempt.type === "MOCK_TEST" ? "Full mock test" : attempt.part || "Part practice";

const percent = (attempt) =>
  Number(attempt.totalQuestions) > 0 && Number.isFinite(Number(attempt.score))
    ? Math.round((Number(attempt.score) / Number(attempt.totalQuestions)) * 100)
    : null;

export default function ProgressPage() {
  const { token, signOut } = useAuth();
  const [history, setHistory] = useState({ status: "loading", attempts: [] });
  const [reloadKey, setReloadKey] = useState(0);
  const [selected, setSelected] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const cancelRef = useRef(null);
  const dialogRef = useRef(null);
  const triggerRef = useRef(null);
  const historyHeadingRef = useRef(null);
  const wasDialogOpenRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setHistory({ status: "loading", attempts: [] });
    listAttempts(token)
      .then((attempts) => {
        if (cancelled) return;
        const completed = Array.isArray(attempts) ? attempts : [];
        setHistory({ status: completed.length ? "ready" : "empty", attempts: completed });
      })
      .catch((error) => {
        if (cancelled) return;
        if (error.status === 401) {
          signOut();
          return;
        }
        setHistory({ status: "error", attempts: [] });
      });
    return () => {
      cancelled = true;
    };
  }, [token, signOut, reloadKey]);

  useEffect(() => {
    if (!selected) {
      if (wasDialogOpenRef.current) {
        wasDialogOpenRef.current = false;
        if (triggerRef.current?.isConnected) triggerRef.current.focus();
        else historyHeadingRef.current?.focus();
      }
      return undefined;
    }

    wasDialogOpenRef.current = true;
    if (deleting) dialogRef.current?.focus();
    else cancelRef.current?.focus();

    const onKeyDown = (event) => {
      if (event.key === "Escape") {
        if (!deleting) closeDialog();
        return;
      }

      if (event.key !== "Tab") return;
      const focusable = dialogRef.current?.querySelectorAll(
        'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );
      if (!focusable?.length) {
        event.preventDefault();
        return;
      }

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && (document.activeElement === first || !dialogRef.current?.contains(document.activeElement))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (document.activeElement === last || !dialogRef.current?.contains(document.activeElement))) {
        event.preventDefault();
        first.focus();
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
    // closeDialog is intentionally local to the active dialog lifecycle.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected, deleting]);

  const summary = useMemo(() => {
    const valid = history.attempts.map(percent).filter((value) => value !== null);
    return {
      count: history.attempts.length,
      average: valid.length ? Math.round(valid.reduce((sum, value) => sum + value, 0) / valid.length) : null,
    };
  }, [history.attempts]);

  function closeDialog() {
    if (deleting) return;
    setSelected(null);
    setDeleteError("");
  }

  async function confirmDelete() {
    if (!selected || deleting) return;
    setDeleting(true);
    setDeleteError("");
    try {
      await deleteAttempt(token, selected.id);
      setHistory((current) => {
        const attempts = current.attempts.filter((attempt) => String(attempt.id) !== String(selected.id));
        return { status: attempts.length ? "ready" : "empty", attempts };
      });
      setSelected(null);
    } catch (error) {
      if (error.status === 401) {
        signOut();
        return;
      }
      setDeleteError("We couldn't delete this attempt. Your history has not been changed. Please try again.");
    } finally {
      setDeleting(false);
    }
  }

  const chronological = [...history.attempts].reverse();
  const points = chronological.map((attempt, index) => ({
    ...attempt,
    value: percent(attempt),
    x: chronological.length === 1 ? 360 : 28 + (index * 664) / (chronological.length - 1),
  })).filter((point) => point.value !== null);
  const polyline = points.map((point) => `${point.x},${180 - point.value * 1.5}`).join(" ");

  return (
    <>
      <div className="page-head">
        <h1>Your progress</h1>
        <p>Review completed TOEIC practices and track your accuracy over time.</p>
      </div>

      {history.status === "loading" && (
        <div role="status" aria-live="polite">
          <span className="visually-hidden">Loading your attempt history…</span>
          <div className="progress-summary" aria-hidden="true">
            <div className="skeleton" style={{ height: 82 }} />
            <div className="skeleton" style={{ height: 82 }} />
          </div>
          <div className="skeleton" style={{ height: 220 }} />
        </div>
      )}

      {history.status === "error" && (
        <div>
          <div className="error-msg" role="alert">We couldn't load your attempt history. Please try again.</div>
          <button type="button" className="btn btn-secondary" onClick={() => setReloadKey((key) => key + 1)}>
            Try again
          </button>
        </div>
      )}

      {history.status === "empty" && (
        <div className="card empty-state">
          <p>You haven't completed a practice attempt yet.</p>
          <p style={{ marginTop: 8 }}>Complete a practice to see your score history here.</p>
          <div className="q-actions" style={{ justifyContent: "center", marginLeft: "auto", marginRight: "auto" }}>
            <Link to="/dashboard" className="btn btn-primary">Start your first practice</Link>
          </div>
        </div>
      )}

      {history.status === "ready" && (
        <>
          <section className="progress-summary" aria-label="Progress summary">
            <div className="stat">
              <p className="label">Completed attempts</p>
              <p className="value">{summary.count}</p>
            </div>
            <div className="stat">
              <p className="label">Average accuracy</p>
              <p className="value">{summary.average === null ? "—" : `${summary.average}%`}</p>
            </div>
          </section>

          <section className="card trend-card" aria-labelledby="trend-heading">
            <div className="trend-heading">
              <div>
                <h2 id="trend-heading">Score trend</h2>
                <p>Percentage correct per completed attempt</p>
              </div>
            </div>
            {points.length === 0 ? (
              <p className="stats-note">There are no scored attempts to chart yet.</p>
            ) : (
              <>
                <svg className="trend-chart" viewBox="0 0 720 210" role="img" aria-label="Accuracy trend from oldest to newest attempt">
                  {[30, 75, 120, 180].map((y) => (
                    <line key={y} x1="20" x2="700" y1={y} y2={y} className="trend-gridline" />
                  ))}
                  {points.length > 1 && <polyline points={polyline} className="trend-line" />}
                  {points.map((point) => (
                    <circle key={point.id} cx={point.x} cy={180 - point.value * 1.5} r="5" className="trend-point">
                      <title>{formatDate(point.startedAt)}: {point.value}%</title>
                    </circle>
                  ))}
                </svg>
                <div className="trend-labels"><span>Oldest</span><span>Most recent</span></div>
              </>
            )}
          </section>

          <section aria-labelledby="history-heading">
            <div className="history-card card">
              <div className="history-title">
                <h2 id="history-heading" ref={historyHeadingRef} tabIndex={-1}>Attempt history</h2>
                <span className="history-count">{history.attempts.length} {history.attempts.length === 1 ? "attempt" : "attempts"}</span>
              </div>
              <ul className="history-list">
                {history.attempts.map((attempt) => {
                  const accuracy = percent(attempt);
                  const name = attemptName(attempt);
                  const deletingThis = deleting && String(selected?.id) === String(attempt.id);
                  return (
                    <li className="history-row" key={attempt.id}>
                      <div className="history-meta">
                        <p className="history-name">{name}</p>
                        <p className="history-date">{formatDate(attempt.startedAt)} · Completed</p>
                      </div>
                      <div className="history-score">
                        <strong>{attempt.score ?? "—"} / {attempt.totalQuestions ?? "—"}</strong>
                        <span>{accuracy === null ? "Score unavailable" : `${accuracy}% correct`}</span>
                      </div>
                      <button
                        type="button"
                        className="icon-btn"
                        aria-label={`Delete attempt: ${name}, ${formatDate(attempt.startedAt)}, score ${attempt.score ?? "unavailable"}`}
                        disabled={deleting}
                        onClick={(event) => { triggerRef.current = event.currentTarget; setSelected(attempt); setDeleteError(""); }}
                      >
                        <span aria-hidden="true">×</span>
                      </button>
                      {deletingThis && <span className="visually-hidden">Deleting this attempt…</span>}
                    </li>
                  );
                })}
              </ul>
            </div>
          </section>
        </>
      )}

      {selected && (
        <div className="delete-backdrop">
          <section
            ref={dialogRef}
            className="delete-dialog"
            role="dialog"
            tabIndex={-1}
            aria-modal="true"
            aria-labelledby="delete-heading"
            aria-describedby="delete-description"
          >
            <h2 id="delete-heading">Delete this attempt?</h2>
            <p id="delete-description">
              This will permanently remove your {attemptName(selected)} from history, including its saved answers.
              This action cannot be undone.
            </p>
            {deleteError && <div className="error-msg" role="alert">{deleteError}</div>}
            <div className="delete-dialog-actions">
              <button ref={cancelRef} type="button" className="btn btn-secondary" disabled={deleting} onClick={closeDialog}>
                Cancel
              </button>
              <button type="button" className="btn btn-danger" disabled={deleting} onClick={confirmDelete}>
                {deleting ? "Deleting…" : "Delete attempt"}
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}
