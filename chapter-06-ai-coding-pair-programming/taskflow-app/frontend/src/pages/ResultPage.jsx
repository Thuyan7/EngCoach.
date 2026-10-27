import { useCallback, useEffect, useState } from "react";
import { Link, Navigate, useLocation, useParams } from "react-router-dom";
import { getAttempt, retryExplanation, submitAttempt } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const letter = (i) => String.fromCharCode(65 + i);

// S9. Shows the graded result of a submitted attempt.
//  - Score and per-answer explanations come from POST /attempts/{id}/submit
//    (S8a/S8b). Arriving from the test screen, that response is passed in the
//    navigation state. After a refresh or a revisit there is no state, so the
//    page calls the same endpoint again — safe because submitting an
//    already-submitted attempt is idempotent (same stored score and
//    explanations, nothing regraded). An attempt that is NOT submitted is
//    sent back to the test screen first, so this page can never grade one.
//  - Question text and options come from GET /attempts/{id}.
export default function ResultPage() {
  const { attemptId } = useParams();
  const location = useLocation();
  const { token, signOut } = useAuth();

  const [load, setLoad] = useState({ status: "loading" });
  const [attempt, setAttempt] = useState(null);
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState({}); // questionId -> {status:"retrying"|"failed"}
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoad({ status: "loading" });
    (async () => {
      try {
        const detail = await getAttempt(token, attemptId);
        if (cancelled) return;
        if (detail.status !== "SUBMITTED") {
          setLoad({ status: "notSubmitted" });
          return;
        }
        const graded = location.state?.result ?? (await submitAttempt(token, attemptId));
        if (cancelled) return;
        setAttempt(detail);
        setResult(graded);
        setLoad({ status: "ready" });
      } catch (err) {
        if (cancelled) return;
        if (err.status === 401) return signOut();
        if (err.status === 403 || err.status === 404) setLoad({ status: "unavailable" });
        else setLoad({ status: "error" });
      }
    })();
    return () => {
      cancelled = true;
    };
    // location.state is read once on arrival; reloads re-fetch from the server.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attemptId, token, signOut, reloadKey]);

  const retryOne = useCallback(
    async (questionId) => {
      setRetry((r) => ({ ...r, [questionId]: { status: "retrying" } }));
      try {
        const refreshed = await retryExplanation(token, attemptId, questionId);
        setResult((prev) => ({
          ...prev,
          answers: prev.answers.map((a) =>
            a.questionId === questionId
              ? { ...a, explanationStatus: refreshed.explanationStatus, explanationText: refreshed.explanationText }
              : a,
          ),
        }));
        // Still UNAVAILABLE after the retry is not an HTTP error: say so.
        setRetry((r) => ({
          ...r,
          [questionId]: refreshed.explanationStatus === "OK" ? undefined : { status: "failed" },
        }));
      } catch (err) {
        if (err.status === 401) return signOut();
        setRetry((r) => ({ ...r, [questionId]: { status: "failed" } }));
      }
    },
    [token, attemptId, signOut],
  );

  if (load.status === "loading") {
    return (
      <div role="status" aria-live="polite">
        <span className="visually-hidden">Loading your result…</span>
        <div className="skeleton" style={{ height: 24, width: 220, marginBottom: 16 }} aria-hidden="true" />
        <div className="skeleton" style={{ height: 160 }} aria-hidden="true" />
      </div>
    );
  }

  if (load.status === "notSubmitted") return <Navigate to={`/practice/${attemptId}`} replace />;

  if (load.status === "unavailable") {
    return (
      <div className="card empty-state">
        <p>This result isn't available.</p>
        <Link to="/dashboard" className="btn btn-secondary">
          Back to Dashboard
        </Link>
      </div>
    );
  }

  if (load.status === "error") {
    return (
      <div>
        <div className="error-msg" role="alert">
          We couldn't load your result. Please try again.
        </div>
        <button type="button" className="btn btn-secondary" onClick={() => setReloadKey((k) => k + 1)}>
          Try again
        </button>
      </div>
    );
  }

  const { score, totalQuestions } = result;
  const incorrect = totalQuestions - score;
  const percent = totalQuestions > 0 ? Math.round((score / totalQuestions) * 100) : null;
  const answerFor = new Map(result.answers.map((a) => [a.questionId, a]));

  // Every question not answered correctly, in test order: a wrong answer, or
  // no answer at all (an unanswered question has no Answer row and counts as
  // incorrect, per the S8a contract).
  const mistakes = attempt.questions
    .map((question, i) => ({ question, number: i + 1, answer: answerFor.get(question.id) }))
    .filter(({ answer }) => !answer || !answer.isCorrect);

  const title = attempt.type === "MOCK_TEST" ? "Mock test result" : `${attempt.part} result`;

  return (
    <>
      <div className="page-head">
        <h1>{title}</h1>
        <p>Submitted just now</p>
      </div>

      <div className="card result-score-card">
        <p className="label">Your score</p>
        <p className="result-big">
          {score} / {totalQuestions}
        </p>
        <p className="stats-note">{percent === null ? "No questions were graded." : `${percent}% correct`}</p>
        <div className="result-split">
          <div>
            <span className="rs-label">Correct</span>
            <span className="rs-value">{score}</span>
          </div>
          <div>
            <span className="rs-label">Incorrect</span>
            <span className="rs-value">{incorrect}</span>
          </div>
        </div>
      </div>

      <div className="section-head">
        <h2>Review your mistakes</h2>
      </div>

      {mistakes.length === 0 ? (
        <div className="card empty-state">
          <p>No mistakes — every answer was correct.</p>
        </div>
      ) : (
        <ul className="explain-list">
          {mistakes.map(({ question, number, answer }) => (
            <li key={question.id}>
              <MistakeCard
                question={question}
                number={number}
                answer={answer}
                retry={retry[question.id]}
                onRetry={() => retryOne(question.id)}
              />
            </li>
          ))}
        </ul>
      )}

      <div className="q-actions">
        <Link to="/dashboard" className="btn btn-primary">
          Back to Dashboard
        </Link>
      </div>
    </>
  );
}

function MistakeCard({ question, number, answer, retry, onRetry }) {
  const chosen = answer?.chosenOption;
  const chosenIndex = chosen == null ? -1 : question.options.indexOf(chosen);
  const heading = !answer
    ? `Question ${number} — Not answered`
    : `Question ${number} — Incorrect (you chose ${chosenIndex >= 0 ? `${letter(chosenIndex)}. ` : ""}${chosen})`;

  const unavailable = answer?.explanationStatus === "UNAVAILABLE";
  const hasExplanation = answer?.explanationStatus === "OK" && answer.explanationText;

  return (
    <div className={`explain-card ${unavailable ? "unavailable" : "incorrect"}`}>
      <p className="q">{heading}</p>
      <p className="body prompt">{question.prompt}</p>
      {hasExplanation && <p className="body">{answer.explanationText}</p>}
      {unavailable && (
        <>
          <p className="body">The explanation for this question is unavailable right now.</p>
          {retry?.status === "failed" && (
            <p className="field-error" role="alert">
              Still unavailable. Please try again in a moment.
            </p>
          )}
          <button type="button" className="link-btn" disabled={retry?.status === "retrying"} onClick={onRetry}>
            {retry?.status === "retrying" ? "Retrying…" : "Retry"}
          </button>
        </>
      )}
    </div>
  );
}
