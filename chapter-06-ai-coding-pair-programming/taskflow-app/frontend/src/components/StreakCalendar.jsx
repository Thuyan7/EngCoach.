import { useEffect, useState } from "react";
import { getStreak } from "../api/attempts.js";
import { useAuth } from "../auth/AuthContext.jsx";

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

// "2026-11" -> { year: 2026, month: 11 }
function parseMonth(text) {
  const parts = text.split("-");
  return { year: Number(parts[0]), month: Number(parts[1]) };
}

// "2026-11" moved by `step` months, as "yyyy-MM".
function shiftMonth(text, step) {
  const { year, month } = parseMonth(text);
  const moved = new Date(year, month - 1 + step, 1);
  return `${moved.getFullYear()}-${String(moved.getMonth() + 1).padStart(2, "0")}`;
}

// Study streak with a calendar that marks the days the learner answered questions.
// It is an extra: if the streak cannot be loaded the card is simply not shown.
export default function StreakCalendar() {
  const { token, signOut } = useAuth();
  const [month, setMonth] = useState(null); // null = the current month, as decided by the server
  const [data, setData] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getStreak(token, month)
      .then((result) => {
        if (!cancelled && result && Array.isArray(result.studyDays) && result.month) setData(result);
      })
      .catch((error) => {
        if (!cancelled && error.status === 401) signOut();
      });
    return () => {
      cancelled = true;
    };
  }, [token, signOut, month]);

  if (!data) return null;

  const { year, month: monthNumber } = parseMonth(data.month);
  const firstWeekday = (new Date(year, monthNumber - 1, 1).getDay() + 6) % 7; // Monday = 0
  const daysInMonth = new Date(year, monthNumber, 0).getDate();
  const monthLabel = new Date(year, monthNumber - 1, 1).toLocaleString("en-US", { month: "long", year: "numeric" });
  const isCurrentMonth = data.month === data.today.slice(0, 7);

  const cells = [];
  for (let i = 0; i < firstWeekday; i++) {
    cells.push(<li key={`blank-${i}`} className="cal-blank" aria-hidden="true" />);
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const iso = `${data.month}-${String(day).padStart(2, "0")}`;
    const studied = data.studyDays.includes(iso);
    const isToday = iso === data.today;
    const classes = ["cal-day"];
    if (studied) classes.push("studied");
    if (isToday) classes.push("today");
    const label = `${day} ${monthLabel}${studied ? ", studied" : ""}${isToday ? ", today" : ""}`;
    cells.push(
      <li key={iso} className={classes.join(" ")} aria-label={label}>
        <span aria-hidden="true">{day}</span>
      </li>,
    );
  }

  const streakText = data.currentStreak === 1 ? "1 day" : `${data.currentStreak} days`;
  const longestText = data.longestStreak === 1 ? "1 day" : `${data.longestStreak} days`;
  let hint = "Answer a question today to start a streak.";
  if (data.studiedToday) hint = "You studied today. Keep it going tomorrow.";
  else if (data.currentStreak > 0) hint = "Answer a question today to keep your streak alive.";

  return (
    <section className="card streak-card" aria-label="Study streak">
      <div className="streak-summary">
        <div>
          <p className="label">Study streak</p>
          <p className="streak-value">{streakText}</p>
          <p className="streak-note">Longest: {longestText}</p>
          <p className="streak-note">{hint}</p>
        </div>
      </div>

      <div className="streak-calendar">
        <div className="cal-head">
          <button type="button" className="btn btn-secondary" aria-label="Previous month" onClick={() => setMonth(shiftMonth(data.month, -1))}>
            &lsaquo;
          </button>
          <strong>{monthLabel}</strong>
          <button type="button" className="btn btn-secondary" aria-label="Next month" disabled={isCurrentMonth} onClick={() => setMonth(shiftMonth(data.month, 1))}>
            &rsaquo;
          </button>
        </div>
        <ul className="cal-weekdays" aria-hidden="true">
          {WEEKDAYS.map((name) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
        <ul className="cal-grid">{cells}</ul>
        <p className="cal-legend"><span className="cal-swatch" aria-hidden="true" /> Days you answered at least one question</p>
      </div>
    </section>
  );
}
