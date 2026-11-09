const PART_NAMES = {
  "Part 1": "Photographs",
  "Part 2": "Question–Response",
  "Part 3": "Conversations",
  "Part 4": "Short Talks",
  "Part 5": "Incomplete Sentences",
  "Part 6": "Text Completion",
  "Part 7": "Reading Comprehension",
};

// Valid only if the API returned the expected shape (a mocked or older server may not).
export const validParts = (data) => (Array.isArray(data?.parts) ? data.parts.filter((p) => p && p.part) : []);

export function EstimateCards({ estimate }) {
  if (!estimate || estimate.total == null) return null;
  return (
    <section className="card estimate-card" aria-label="Estimated TOEIC score">
      <p className="label">Estimated TOEIC score</p>
      <p className="estimate-total">{estimate.total} <small>/ 990</small></p>
      <div className="result-split">
        <div><span className="rs-label">Listening</span><span className="rs-value">{estimate.listening} / 495</span></div>
        <div><span className="rs-label">Reading</span><span className="rs-value">{estimate.reading} / 495</span></div>
      </div>
      <p className="stats-note">A rough estimate from your accuracy. The official TOEIC conversion tables are not public, so your real score may differ.</p>
    </section>
  );
}

export function PartBars({ parts, weakestPart, heading = "Accuracy by Part" }) {
  const rows = validParts(parts ? { parts } : null);
  if (rows.length === 0) return null;
  return (
    <section className="card part-card" aria-label={heading}>
      <h2>{heading}</h2>
      <ul className="part-bars">
        {rows.map((p) => (
          <li key={p.part} className={p.part === weakestPart ? "weakest" : ""}>
            <div className="part-bar-head">
              <span><strong>{p.part}</strong> <small>{PART_NAMES[p.part] || ""}</small></span>
              <span>{p.correct} / {p.total} · {p.accuracyPercent}%</span>
            </div>
            <div className="part-bar" role="img" aria-label={`${p.part} accuracy ${p.accuracyPercent}%`}>
              <span style={{ width: `${Math.max(0, Math.min(100, p.accuracyPercent))}%` }} />
            </div>
          </li>
        ))}
      </ul>
      {weakestPart && <p className="stats-note weakest-note">{weakestPart} needs the most practice.</p>}
    </section>
  );
}
