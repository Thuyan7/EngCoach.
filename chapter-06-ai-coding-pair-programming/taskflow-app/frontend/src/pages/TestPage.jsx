import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getAttempt, saveAnswer, submitAttempt } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

// S7b. Everything comes from the API: GET /attempts/{id} returns the
// questions (id, prompt, options — never the answer key), the saved answers
// and the status; each answer is auto-saved with PUT.
const letter = (i) => String.fromCharCode(65 + i);
const formatTime = (seconds) => {
  const safeSeconds = Math.max(0, Math.floor(seconds));
  const minutes = Math.floor(safeSeconds / 60);
  const remainder = safeSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`;
};
export default function TestPage() {
  const { attemptId } = useParams();
  const { token, signOut } = useAuth();
  const navigate = useNavigate();

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
  // Questions whose latest save failed; a submit is blocked while any remain.
  const failedSaves = useRef(new Set());
  const [submitState, setSubmitState] = useState({ status: "idle" }); // idle|confirming|grading|error
  const [remainingSeconds, setRemainingSeconds] = useState(null);
  const submissionLock = useRef(false);
  const autoSubmitTriggered = useRef(false);

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
        setRemainingSeconds(detail.remainingSeconds ?? null);
        autoSubmitTriggered.current = false;
        submissionLock.current = false;
        setSubmitState({ status: "idle" });
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
        .then(() => {
          failedSaves.current.delete(questionId);
          setSaveState((s) => ({ ...s, [questionId]: "saved" }));
        })
        .catch((err) => {
          if (err.status === 401) return signOut();
          failedSaves.current.add(questionId);
          setSaveState((s) => ({ ...s, [questionId]: "error" }));
        });
    },
    [token, attemptId, signOut],
  );

  async function submit(force = false) {
    if (submissionLock.current) return;
    submissionLock.current = true;
    setSubmitState({ status: "grading" });
    // Let every in-flight answer save finish first, so nothing typed a moment
    // ago is missing when the server grades.
    await Promise.all(Object.values(chains.current));
    if (!force && failedSaves.current.size > 0) {
      submissionLock.current = false;
      setSubmitState({ status: "error", reason: "unsaved" });
      return;
    }
    try {
      const result = await submitAttempt(token, attemptId);
      navigate(`/result/${attemptId}`, { state: { result } });
    } catch (err) {
      if (err.status === 401) return signOut();
      submissionLock.current = false;
      setSubmitState({ status: "error", reason: "grading" });
    }
  }

  // Use the server-computed remaining time so resuming a mock test does not
  // restart its two-hour clock.
  useEffect(() => {
    if (
      load.status !== "ready" ||
      attempt?.type !== "MOCK_TEST" ||
      attempt.status !== "IN_PROGRESS" ||
      remainingSeconds === null ||
      submitState.status === "grading"
    ) {
      return undefined;
    }

    if (remainingSeconds <= 0) {
      if (!autoSubmitTriggered.current) {
        autoSubmitTriggered.current = true;
        void submit(true);
      }
      return undefined;
    }

    const intervalId = setInterval(() => {
      setRemainingSeconds((seconds) => (seconds === null ? null : Math.max(0, seconds - 1)));
    }, 1000);
    return () => clearInterval(intervalId);
  }, [load.status, attempt, remainingSeconds, submitState.status]);

  function startSubmit() {
    const unanswered = questions.filter((q) => answers[q.id] === undefined).length;
    if (unanswered > 0) setSubmitState({ status: "confirming" });
    else submit();
  }

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
        <h1>{attempt.type === "MOCK_TEST" ? "Mock test" : `${attempt.part} practice`}</h1>
        <p>
          {answeredCount} of {questions.length} answered
        </p>
        {attempt.type === "MOCK_TEST" && remainingSeconds !== null && attempt.status === "IN_PROGRESS" && (
          <p
            className={`save-status countdown${remainingSeconds <= 60 ? " countdown--critical" : remainingSeconds <= 600 ? " countdown--warning" : ""}`}
            role="timer"
            aria-label="Time remaining"
            data-time-state={remainingSeconds <= 60 ? "critical" : remainingSeconds <= 600 ? "warning" : "normal"}
            Time remaining: {formatTime(remainingSeconds)}
          </p>
        )}
      </div>

      {locked && (
        <div className="sample-banner" role="note">
          This attempt has already been submitted, so answers can no longer be changed.{" "}
          <Link to={`/result/${attempt.id}`}>View result</Link>
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
          {question.passage && (
            <section className="question-passage" aria-label="Reading passage">
              <h2>Read the passage</h2>
              <p>{question.passage}</p>
            </section>
          )}
          {question.imageUrl && (
            <img
              className="question-media-image"
              src={question.imageUrl}
              alt={`Illustration for question ${index + 1}`}
              loading="lazy"
            />
          )}
          {question.audioUrl && (
            <div className="question-media-audio">
              <audio controls preload="none" src={question.audioUrl} aria-label={`Audio for question ${index + 1}`}>
                Your browser does not support audio playback.
              </audio>
            </div>
          )}
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

      {!locked && (
        <div className="submit-area">
          {submitState.status === "confirming" && (
            <div className="submit-confirm" role="group" aria-label="Confirm submission">
              <p>
                You have {questions.length - answeredCount} unanswered question
                {questions.length - answeredCount === 1 ? "" : "s"}. They will be marked incorrect.
              </p>
              <div className="q-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setSubmitState({ status: "idle" })}>
                  Keep working
                </button>
                <button type="button" className="btn btn-primary" onClick={submit}>
                  Submit anyway
                </button>
              </div>
            </div>
          )}

          {submitState.status === "error" && (
            <div className="error-msg" role="alert">
              {submitState.reason === "unsaved"
                ? "Some answers couldn't be saved yet. Use Retry on them, then submit again."
                : "We couldn't grade your attempt. Your answers are saved — please try again."}
            </div>
          )}

          {submitState.status === "grading" && (
            <p className="save-status" role="status" aria-live="polite">
              Grading your answers… explanations can take a few seconds.
            </p>
          )}

          {submitState.status !== "confirming" && (
            <button
              type="button"
              className="btn btn-primary"
              disabled={submitState.status === "grading"}
              onClick={startSubmit}
            >
              {submitState.status === "grading" ? "Grading…" : "Submit for grading"}
            </button>
          )}
        </div>
      )}
    </>
  );
}
