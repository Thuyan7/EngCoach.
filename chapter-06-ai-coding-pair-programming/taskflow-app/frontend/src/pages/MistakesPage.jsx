import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMistakes } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const letter = (i) => String.fromCharCode(65 + i);

// Review of questions answered wrongly. A question disappears from the list once it is answered correctly later.
export default function MistakesPage() {
  const { token, signOut } = useAuth();
  const [part, setPart] = useState("");
  const [data, setData] = useState(null);
  const [status, setStatus] = useState("loading");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setStatus("loading");
    getMistakes(token, part)
      .then((result) => {
        if (cancelled) return;
        setData(result);
        setStatus("ready");
      })
      .catch((error) => {
        if (cancelled) return;
        if (error.status === 401) {
          signOut();
          return;
        }
        setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [token, signOut, part, reloadKey]);

  const partCounts = data && Array.isArray(data.partCounts) ? data.partCounts : [];
  const mistakes = data && Array.isArray(data.mistakes) ? data.mistakes : [];

  return (
    <>
      <div className="page-head">
        <h1>Review your mistakes</h1>
        <p>Questions you got wrong the last time you answered them. Answer one correctly later and it leaves this list.</p>
      </div>

      {status === "loading" && <p role="status">Loading your mistakes…</p>}

      {status === "error" && (
        <div>
          <div className="error-msg" role="alert">We couldn't load your mistakes. Please try again.</div>
          <button type="button" className="btn btn-secondary" onClick={() => setReloadKey(reloadKey + 1)}>
            Try again
          </button>
        </div>
      )}

      {status === "ready" && data.total === 0 && (
        <div className="card empty-state">
          <p>You have no mistakes to review.</p>
          <p style={{ marginTop: 8 }}>Complete a practice and the questions you miss will show up here.</p>
          <div className="q-actions" style={{ justifyContent: "center" }}>
            <Link to="/practice" className="btn btn-primary">Start practicing</Link>
          </div>
        </div>
      )}

      {status === "ready" && data.total > 0 && (
        <>
          <div className="filter-row" role="group" aria-label="Filter by Part">
            <button type="button" className={part === "" ? "chip chip-active" : "chip"} aria-pressed={part === ""} onClick={() => setPart("")}>
              All ({data.total})
            </button>
            {partCounts.map((item) => (
              <button
                key={item.part}
                type="button"
                className={part === item.part ? "chip chip-active" : "chip"}
                aria-pressed={part === item.part}
                onClick={() => setPart(item.part)}
              >
                {item.part} ({item.count})
              </button>
            ))}
          </div>

          <ul className="explain-list">
            {mistakes.map((item) => (
              <li key={item.question.id}>
                <MistakeCard item={item} />
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  );
}

function MistakeCard({ item }) {
  const question = item.question;
  const context = question.groupContent || question.passage;
  return (
    <div className="explain-card incorrect">
      <p className="q">{item.part}</p>
      {question.imageUrl && <img className="mistake-image" src={question.imageUrl} alt="Photo for this question" />}
      {context && <p className="body mistake-context">{context}</p>}
      {question.audioText && <p className="body mistake-context"><strong>Audio script:</strong> {question.audioText}</p>}
      <p className="body prompt">{question.prompt}</p>
      <ul className="mistake-options">
        {question.options.map((option, index) => {
          const isCorrect = option === question.correctOption;
          const isChosen = option === item.chosenOption;
          const tag = isCorrect ? " (correct answer)" : isChosen ? " (your answer)" : "";
          return (
            <li key={index} className={isCorrect ? "is-correct" : isChosen ? "is-chosen" : ""}>
              {letter(index)}. {option}{tag}
            </li>
          );
        })}
      </ul>
      {item.explanationText && <p className="body explanation-text">{item.explanationText}</p>}
    </div>
  );
}
