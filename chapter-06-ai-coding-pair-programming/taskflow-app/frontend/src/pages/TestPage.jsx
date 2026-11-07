import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getAttempt, saveAnswer, submitAttempt } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

// S7b. Everything comes from the API: GET /attempts/{id} returns the
// questions (id, prompt, options — never the answer key), the saved answers
// and the status; each answer is auto-saved with PUT.
const letter = (i) => String.fromCharCode(65 + i);
const MOCK_TEST_DURATION_SECONDS = 7200;
const TIMER_RADIUS = 46;
const TIMER_CIRCUMFERENCE = 2 * Math.PI * TIMER_RADIUS;
const formatTime = (seconds) => {
  const safeSeconds = Math.max(0, Math.floor(seconds));
  const minutes = Math.floor(safeSeconds / 60);
  const remainder = safeSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`;
};

// Part 3, 4, 6 and 7: the questions of one conversation / talk / passage share the same audio or text,
// so they are shown together on one screen instead of one per "Next".
const GROUPED_TYPES = ["CONVERSATION", "TALK", "PASSAGE"];

// Returns the screens of the test: each screen is a list of question positions.
function buildSteps(questions) {
  const steps = [];
  for (let i = 0; i < questions.length; i++) {
    const q = questions[i];
    const last = steps[steps.length - 1];
    let joins = false;
    if (last && q.groupId != null && GROUPED_TYPES.includes(q.groupType)) {
      const before = questions[last.indexes[last.indexes.length - 1]];
      joins = before.groupId === q.groupId && before.groupType === q.groupType;
    }
    if (joins) last.indexes.push(i);
    else steps.push({ indexes: [i] });
  }
  return steps;
}

function playQuestionAudio(text) {
  if (typeof window === "undefined" || !window.speechSynthesis || typeof window.SpeechSynthesisUtterance !== "function") {
    return;
  }
  window.speechSynthesis.cancel();
  const spokenText = text.replace(/(?:^|\s)[MW]:\s*/g, " ");
  const utterance = new window.SpeechSynthesisUtterance(spokenText);
  utterance.lang = "en-US";
  utterance.rate = 0.92;
  window.speechSynthesis.speak(utterance);
}

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
        const loadedSteps = buildSteps(qs);
        const firstOpen = loadedSteps.findIndex((st) => st.indexes.some((i) => saved[qs[i].id] === undefined));
        setIndex(firstOpen === -1 ? Math.max(loadedSteps.length - 1, 0) : firstOpen);
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
      !["MOCK_TEST", "TEST"].includes(attempt?.type) ||
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

  const steps = buildSteps(questions);
  const stepIndex = Math.min(index, steps.length - 1);
  const stepQuestionPositions = steps[stepIndex].indexes;
  const stepQuestions = stepQuestionPositions.map((i) => questions[i]);
  const question = stepQuestions[0]; // shared media (passage, image, audio) comes from the first question
  const firstNumber = stepQuestionPositions[0] + 1;
  const lastNumber = stepQuestionPositions[stepQuestionPositions.length - 1] + 1;
  const stepOfQuestion = [];
  steps.forEach((st, si) => st.indexes.forEach((i) => (stepOfQuestion[i] = si)));
  const locked = attempt.status !== "IN_PROGRESS";
  // Part 2 is shown like the real TOEIC: only the letters A, B, C. The question and the answers are heard, not read.
  // A reading passage group (Part 6, 7) keeps its text in groupContent; that text is read on screen, never played as audio.
  const listeningText = question.audioText || (question.groupType === "PASSAGE" ? null : question.groupContent);
  const hasListeningSource = Boolean(question.audioUrl || listeningText);
  const lettersOnly = hasListeningSource && (question.groupType === "LISTENING_ITEM" || attempt.part === "Part 2");
  const answeredCount = questions.filter((q) => answers[q.id] !== undefined).length;

  return (
    <>
      <div className={`page-head${["MOCK_TEST", "TEST"].includes(attempt.type) ? " mock-test-head" : ""}`}>
        <div className="mock-test-heading">
          <h1>{attempt.type === "MOCK_TEST" ? "Mock test" : attempt.type === "TEST" ? (attempt.testTitle || "TOEIC Test") : `${attempt.part} practice`}</h1>
          <p>
            {answeredCount} of {questions.length} answered
          </p>
          {attempt.type === "TEST" && Array.isArray(attempt.selectedParts) && attempt.selectedParts.length > 0 && (
            <p className="muted-label" aria-label="Selected Test Parts">
              Selected: {attempt.selectedParts.join(", ")}
            </p>
          )}
        </div>
        {["MOCK_TEST", "TEST"].includes(attempt.type) && remainingSeconds !== null && attempt.status === "IN_PROGRESS" && (
          <div
            className="mock-test-timer"
            role="timer"
            aria-label="Time remaining"
            data-time-state={remainingSeconds < 60 ? "critical" : remainingSeconds < 300 ? "warning" : "normal"}
          >
            <span className="visually-hidden">Time remaining: {formatTime(remainingSeconds)}</span>
            <div className="timer-wrap">
              <svg className="timer-ring" viewBox="0 0 104 104" aria-hidden="true" data-testid="mock-timer-ring">
                <circle className="timer-ring-track" cx="52" cy="52" r="46" />
                <circle
                  className={`timer-ring-value${remainingSeconds < 60 ? " timer-ring-value--danger" : remainingSeconds < 300 ? " timer-ring-value--warning" : ""}`}
                  cx="52"
                  cy="52"
                  r="46"
                  strokeDasharray={TIMER_CIRCUMFERENCE}
                  strokeDashoffset={TIMER_CIRCUMFERENCE * (1 - Math.min(remainingSeconds, attempt.timeLimitSeconds || MOCK_TEST_DURATION_SECONDS) / (attempt.timeLimitSeconds || MOCK_TEST_DURATION_SECONDS))}
                  data-testid="mock-timer-ring-value"
                />
              </svg>
              <span className={`timer-digits${remainingSeconds < 60 ? " timer-digits--danger" : remainingSeconds < 300 ? " timer-digits--warning" : ""}`}>
                {formatTime(remainingSeconds)}
              </span>
            </div>
            <p className="timer-label">Time remaining</p>
          </div>
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
            aria-current={stepOfQuestion[i] === stepIndex ? "step" : undefined}
            aria-label={`Question ${i + 1}${answers[q.id] !== undefined ? ", answered" : ""}`}
            data-answered={answers[q.id] !== undefined}
            onClick={() => setIndex(stepOfQuestion[i])}
          >
            {i + 1}
          </button>
        ))}
      </nav>

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
            alt={`Illustration for question ${firstNumber}`}
            loading="lazy"
          />
        )}
        {listeningText && !question.audioUrl && (
          <div className="question-media-audio">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => playQuestionAudio(listeningText)}
              aria-label={question.groupType ? `Play listening audio for ${question.groupType.toLowerCase()}` : "Play listening audio"}
            >
              Play listening audio
            </button>
            <span className="muted-label">Computer-generated voice</span>
          </div>
        )}
        {question.audioUrl && (
          <div className="question-media-audio">
            <audio
              controls
              preload="none"
              src={question.audioUrl}
              aria-label={firstNumber === lastNumber ? `Audio for question ${firstNumber}` : `Audio for questions ${firstNumber} to ${lastNumber}`}
            >
              Your browser does not support audio playback.
            </audio>
          </div>
        )}

        {stepQuestions.map((q, n) => {
          const number = stepQuestionPositions[n] + 1;
          const state = saveState[q.id];
          return (
            <div key={q.id} className="question-block">
              <fieldset className="question">
                <legend>
                  <span className="q-count">
                    Question {number} of {questions.length}
                  </span>
                  {!lettersOnly && <span className="q-prompt">{q.prompt}</span>}
                </legend>
                <div className={lettersOnly ? "q-letter-row" : undefined}>
                  {q.options.map((opt, i) => (
                    <label key={i} className={`q-option${lettersOnly ? " q-option--letter" : ""}`}>
                      <input
                        type="radio"
                        name={`q-${q.id}`}
                        value={opt}
                        aria-label={lettersOnly ? `Option ${letter(i)}` : undefined}
                        checked={answers[q.id] === opt}
                        disabled={locked}
                        onChange={() => choose(q.id, opt)}
                      />
                      <span>
                        {lettersOnly ? (
                          <strong>{letter(i)}</strong>
                        ) : (
                          <>
                            <strong>{letter(i)}.</strong> {opt}
                          </>
                        )}
                      </span>
                    </label>
                  ))}
                </div>
              </fieldset>

              <div className="save-status" role="status" aria-live="polite">
                {state === "saving" && "Saving…"}
                {state === "saved" && "Saved"}
                {state === "error" && (
                  <span className="save-error">
                    Couldn't save this answer.{" "}
                    <button type="button" className="link-btn" onClick={() => persist(q.id, answers[q.id])}>
                      Retry
                    </button>
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className="q-actions">
        <button
          type="button"
          className="btn btn-secondary"
          disabled={stepIndex === 0}
          onClick={() => setIndex(stepIndex - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className="btn btn-primary"
          disabled={stepIndex >= steps.length - 1}
          onClick={() => setIndex(stepIndex + 1)}
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
