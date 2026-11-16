import { useEffect, useState } from "react";
import { getGoals, saveGoals } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

// Study goals on the Home page: today's questions, study streak and progress to the target score.
// It is an extra, so if the goals cannot be loaded the card is simply not shown.
export default function GoalsCard() {
  const { token, signOut } = useAuth();
  const [goals, setGoals] = useState(null);
  const [editing, setEditing] = useState(false);
  const [targetText, setTargetText] = useState("");
  const [dailyText, setDailyText] = useState("");
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getGoals(token)
      .then((data) => {
        if (!cancelled && data && typeof data.dailyQuestionGoal === "number") setGoals(data);
      })
      .catch((error) => {
        if (!cancelled && error.status === 401) signOut();
      });
    return () => {
      cancelled = true;
    };
  }, [token, signOut]);

  if (!goals) return null;

  function openForm() {
    setTargetText(goals.targetScore == null ? "" : String(goals.targetScore));
    setDailyText(String(goals.dailyQuestionGoal));
    setFormError("");
    setEditing(true);
  }

  async function save(event) {
    event.preventDefault();
    const daily = Number(dailyText);
    if (dailyText.trim() === "" || !Number.isInteger(daily) || daily < 1 || daily > 500) {
      setFormError("Daily questions must be a whole number from 1 to 500.");
      return;
    }
    let target = null;
    if (targetText.trim() !== "") {
      target = Number(targetText);
      if (!Number.isInteger(target) || target < 10 || target > 990) {
        setFormError("Target score must be a whole number from 10 to 990, or left empty.");
        return;
      }
    }
    setSaving(true);
    setFormError("");
    try {
      const updated = await saveGoals(token, target, daily);
      setGoals(updated);
      setEditing(false);
    } catch (error) {
      if (error.status === 401) {
        signOut();
        return;
      }
      setFormError("We couldn't save your goals. Please try again.");
    }
    setSaving(false);
  }

  const streakText = goals.currentStreak === 1 ? "1 day" : `${goals.currentStreak} days`;

  let targetLine;
  if (goals.targetScore == null) {
    targetLine = "No target score yet. Set one to track your progress.";
  } else if (goals.estimatedScore == null) {
    targetLine = `Target ${goals.targetScore}. Finish a Test with Listening and Reading to see your estimate.`;
  } else if (goals.estimatedScore >= goals.targetScore) {
    targetLine = `Estimated ${goals.estimatedScore}, you have reached your target of ${goals.targetScore}.`;
  } else {
    targetLine = `Estimated ${goals.estimatedScore}, ${goals.targetScore - goals.estimatedScore} points to your target of ${goals.targetScore}.`;
  }

  return (
    <section className="card goals-card" aria-label="Your study goals">
      <div className="goals-head">
        <h2>Your study goals</h2>
        {!editing && (
          <button type="button" className="btn btn-secondary" onClick={openForm}>
            Edit goals
          </button>
        )}
      </div>

      <div className="goals-grid">
        <div>
          <p className="label">Today</p>
          <p className="goals-value">{goals.answeredToday} / {goals.dailyQuestionGoal}</p>
          <p className="goals-note">{goals.dailyGoalMet ? "Daily goal reached." : "questions answered"}</p>
        </div>
        <div>
          <p className="label">Study streak</p>
          <p className="goals-value">{streakText}</p>
          <p className="goals-note">Longest: {goals.longestStreak} {goals.longestStreak === 1 ? "day" : "days"}</p>
        </div>
        <div>
          <p className="label">Target score</p>
          <p className="goals-value">{goals.targetScore == null ? "Not set" : goals.targetScore}</p>
          <p className="goals-note">{targetLine}</p>
        </div>
      </div>

      {editing && (
        <form className="goals-form" onSubmit={save}>
          <label>
            Target TOEIC score (optional)
            <input type="number" inputMode="numeric" value={targetText} onChange={(e) => setTargetText(e.target.value)} />
          </label>
          <label>
            Questions per day
            <input type="number" inputMode="numeric" value={dailyText} onChange={(e) => setDailyText(e.target.value)} />
          </label>
          {formError && <div className="error-msg" role="alert">{formError}</div>}
          <div className="q-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? "Saving…" : "Save goals"}</button>
            <button type="button" className="btn btn-secondary" disabled={saving} onClick={() => setEditing(false)}>Cancel</button>
          </div>
        </form>
      )}
    </section>
  );
}
