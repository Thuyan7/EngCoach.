// Pure helper for the Dashboard stat cards, from GET /attempts (S10a: the
// caller's SUBMITTED attempts, each with raw `score` and `totalQuestions`).
//
// "Average score" is the mean of each attempt's percentage correct
// (score / totalQuestions), rounded to a whole percent. A percentage is used
// because attempts have different lengths (a Part vs. a longer test), so
// averaging raw counts would mix scales. Attempts with no usable total
// (null or 0 — e.g. a MOCK_TEST before S11) still count as completed
// attempts but are left out of the average.
export function summarizeAttempts(attempts) {
  const scored = attempts.filter((a) => a.totalQuestions > 0 && typeof a.score === "number");
  const averagePercent =
    scored.length === 0
      ? null
      : Math.round((scored.reduce((sum, a) => sum + a.score / a.totalQuestions, 0) / scored.length) * 100);
  return { attemptCount: attempts.length, averagePercent };
}
