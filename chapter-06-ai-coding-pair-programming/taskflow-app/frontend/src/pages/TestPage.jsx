import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getAttempt, saveAnswer } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

// S7b. Everything comes from the API: GET /attempts/{id} returns the
// questions (id, prompt, options — never the answer key), the saved answers
// and the status; each answer is auto-saved with PUT.
const letter = (i) => String.fromCharCode(65 + i);
export default function TestPage() {
  const { attemptId } = useParams();
  const { token, signOut } = useAuth();

  const [load, setLoad] = useState({ status: "loading" });
  const [attempt, setAttempt] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({}); // questionId -> option key
  const [saveState, setSaveState] = useState({}); // questionId -> saving|saved|error
  const [index, setIndex] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  // One chain of PUTs per question, so rapid re-answers reach the server in
  // the order the user made them.
  const chains = useRef({});

  useEffect(() => {
    let cancelled = false;
    setLoad({ status: "loading" });
    (async () => {
      try {
        const detail = await getAttempt(token, attemptId);
        const qs = detail.questions ?? [];
        if (cancelled) return;
        const saved = {};
        detail.answers.forEach((a) => (saved[a.questionId] = a.chosenOption));
        setAttempt(detail);
        setQuestions(qs);
        setAnswers(saved);
        setSaveState({});
        // Position is not stored server-side; resume at the first question
        // that has no saved answer (or the last one if all are answered).
        const firstOpen = qs.findIndex((q) => saved[q.id] === undefined);
        setIndex(firstOpen === -1 ? Math.max(qs.length - 1, 0) : firstOpen);
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
  }, [attemptId, token, signOut, reloadKey]);

  const persist = useCallback(
    (questionId, option) => {
      setSaveState((s) => ({ ...s, [questionId]: "saving" }));
      const previous = chains.current[questionId] ?? Promise.resolve();
      chains.current[questionId] = previous
        .catch(() => {})
        .then(() => saveAnswer(token, attemptId, questionId, option))
        .then(() => setSaveState((s) => ({ ...s, [questionId]: "saved" })))
        .catch((err) => {
          if (err.status === 401) return signOut();
          setSaveState((s) => ({ ...s, [questionId]: "error" }));
        });
    },
    [token, attemptId, signOut],
  );

  function choose(questionId, option) {
    setAnswers((a) => ({ ...a, [questionId]: option }));
    persist(questionId, option);
  }

  if (load.status === "loading") {
    return (
      <div role="status" aria-live="polite">
        <span className="visually-hidden">Loading your practice…</span>
        <div className="skeleton" style={{ height: 24, width: 220, marginBottom: 16 }} aria-hidden="true" />
        <div className="skeleton" style={{ height: 200 }} aria-hidden="true" />
      </div>
    );
  }

  if (load.status === "unavailable") {
    return (
      <div className="card empty-state">
        <p>This practice isn't available.</p>
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
          We couldn't load your practice. Please try again.
        </div>
        <button type="button" className="btn btn-secondary" onClick={() => setReloadKey((k) => k + 1)}>
          Try again
        </button>
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <div className="card empty-state">
        <p>There are no questions in this practice yet.</p>
        <Link to="/dashboard" className="btn btn-secondary">
          Back to Dashboard
        </Link>
      </div>
    );
  }

  const question = questions[index];
  const locked = attempt.status !== "IN_PROGRESS";
  const state = question ? saveState[question.id] : undefined;
  const answeredCount = questions.filter((q) => answers[q.id] !== undefined).length;

  return (
    <>
      <div className="page-head">
        <h1>{attempt.part} practice</h1>
        <p>
          {answeredCount} of {questions.length} answered
        </p>
      </div>

      {locked && (
        <div className="sample-banner" role="note">
          This attempt has already been submitted, so answers can no longer be changed.
        </div>
      )}

      <nav aria-label="Questions" className="q-nav">
        {questions.map((q, i) => (
          <button
            key={q.id}
            type="button"
            className="q-dot"
            aria-current={i === index ? "step" : undefined}
            aria-label={`Question ${i + 1}${answers[q.id] !== undefined ? ", answered" : ""}`}
            data-answered={answers[q.id] !== undefined}
            onClick={() => setIndex(i)}
          >
            {i + 1}
          </button>
        ))}
      </nav>

      {question && (
        <div className="card question-card">
          <fieldset className="question" key={question.id}>
            <legend>
              <span className="q-count">
                Question {index + 1} of {questions.length}
              </span>
              <span className="q-prompt">{question.prompt}</span>
            </legend>
            {question.options.map((opt, i) => (
              <label key={i} className="q-option">
                <input
                  type="radio"
                  name={`q-${question.id}`}
                  value={opt}
                  checked={answers[question.id] === opt}
                  disabled={locked}
                  onChange={() => choose(question.id, opt)}
                />
                <span>
                  <strong>{letter(i)}.</strong> {opt}
                </span>
              </label>
            ))}
          </fieldset>

          <div className="save-status" role="status" aria-live="polite">
            {state === "saving" && "Saving…"}
            {state === "saved" && "Saved"}
            {state === "error" && (
              <span className="save-error">
                Couldn't save this answer.{" "}
                <button
                  type="button"
                  className="link-btn"
                  onClick={() => persist(question.id, answers[question.id])}
                >
                  Retry
                </button>
              </span>
            )}
          </div>
        </div>
      )}

      <div className="q-actions">
        <button
          type="button"
          className="btn btn-secondary"
          disabled={index === 0}
          onClick={() => setIndex(index - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className="btn btn-primary"
          disabled={index >= questions.length - 1}
          onClick={() => setIndex(index + 1)}
        >
          Next
        </button>
      </div>
    </>
  );
}
