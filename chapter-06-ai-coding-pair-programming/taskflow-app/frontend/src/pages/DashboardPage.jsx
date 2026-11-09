import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { listAttempts, startToeicTest } from "../api/attempts.js";
import { request } from "../api/client.js";
import { useAuth } from "../auth/AuthContext.jsx";
import { summarizeAttempts } from "../stats.js";

const START_FAILED = "We couldn't start this Test. Please try again.";
const TESTS_FAILED = "We couldn't load the Tests. Please try again.";
const NO_TEST_QUESTIONS = "There are no questions for this Test yet.";
const STATS_FAILED = "We couldn't load your stats. Please try again.";

export default function DashboardPage() {
  const { token, signOut } = useAuth();
  const navigate = useNavigate();
  const [tests, setTests] = useState({ status: "loading", items: [] });
  const [testsReload, setTestsReload] = useState(0);
  const [startingTest, setStartingTest] = useState(null);
  const [testStartError, setTestStartError] = useState("");
  const [stats, setStats] = useState({ status: "loading", attempts: [] });
  const [statsReload, setStatsReload] = useState(0);

  // Practice is organised by Test: GET /tests lists every published Test with its Parts.
  useEffect(() => {
    let cancelled = false;
    setTests({ status: "loading", items: [] });
    request("/tests", { token }).then((data) => {
      if (cancelled) return;
      // A non-array reply means the request did not reach the API (e.g. the dev proxy returned the SPA page).
      if (!Array.isArray(data)) { setTests({ status: "error", items: [] }); return; }
      setTests({ status: data.length ? "ready" : "empty", items: data });
    }).catch((err) => {
      if (cancelled) return;
      if (err.status === 401) return signOut();
      setTests({ status: "error", items: [] });
    });
    return () => { cancelled = true; };
  }, [token, signOut, testsReload]);

  useEffect(() => {
    let cancelled = false;
    setStats({ status: "loading", attempts: [] });
    listAttempts(token).then((attempts) => {
      if (!cancelled) setStats({ status: "ready", attempts });
    }).catch((err) => {
      if (cancelled) return;
      if (err.status === 401) return signOut();
      setStats({ status: "error", attempts: [] });
    });
    return () => { cancelled = true; };
  }, [token, signOut, statsReload]);

  async function startTest(test) {
    setTestStartError("");
    setStartingTest(test.id);
    try {
      // 201 either way: a new attempt, or the unfinished one for this Test and Parts.
      const attempt = await startToeicTest(token, test.id, (test.parts || []).map((p) => p.part));
      navigate(`/practice/${attempt.id}`);
    } catch (err) {
      if (err.status === 401) return signOut();
      setTestStartError(err.status === 422 ? NO_TEST_QUESTIONS : START_FAILED);
      setStartingTest(null);
    }
  }

  const summary = summarizeAttempts(stats.attempts);
  const average = stats.status === "loading" ? "…" : stats.status === "error" ? "—" : summary.averagePercent === null ? "—" : `${summary.averagePercent}%`;
  const completed = stats.status === "loading" ? "…" : stats.status === "error" ? "—" : summary.attemptCount;

  return (
    <>
      <section className="home-hero">
        <div className="home-hero-copy">
          <div className="hero-kicker"><span className="hero-kicker-dot" /> YOUR PERSONAL TOEIC LEARNING SPACE</div>
          <h1>Make every practice<br /><span>count.</span></h1>
          <p>Build your confidence one question at a time. Practice with purpose, understand your mistakes, and get closer to your target score.</p>
          <div className="hero-actions">
            <button type="button" className="btn btn-green" onClick={() => navigate("/practice")}>Explore practice tests <span aria-hidden="true">→</span></button>
            <span className="hero-note"><span aria-hidden="true">✓</span> Learn at your own pace</span>
          </div>
        </div>
        <div className="hero-visual" aria-hidden="true">
          <div className="hero-circle hero-circle-back" />
          <div className="hero-circle hero-circle-front" />
          <div className="hero-illustration">
            <svg viewBox="0 0 440 300" fill="none">
              <path d="M46 242h344" stroke="#B7DCCF" strokeWidth="3" strokeLinecap="round"/>
              <path d="M73 237V116h57v121M84 128h35M84 143h35M84 158h35" stroke="#247A66" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M60 116h83v-13H60z" fill="#1DB88A"/>
              <path d="M293 236V96q0-10 10-10h86q10 0 10 10v140" fill="#DDF5EC" stroke="#9CD8C4" strokeWidth="3"/>
              <rect x="309" y="109" width="74" height="10" rx="5" fill="#13B981"/>
              <rect x="309" y="132" width="48" height="8" rx="4" fill="#8FCDB8"/>
              <rect x="309" y="150" width="61" height="8" rx="4" fill="#8FCDB8"/>
              <rect x="309" y="168" width="38" height="8" rx="4" fill="#8FCDB8"/>
              <path d="M174 238c-12-32-5-78 15-100 14-15 36-18 52-7 18 12 24 38 19 62l-6 45h-80Z" fill="#18364C"/>
              <path d="M180 139c-4-25 13-48 39-50 24-2 43 16 43 39 0 17-9 29-20 37l-52-2c-5-7-8-15-10-24Z" fill="#F4B88F"/>
              <path d="M178 138c-15-28 4-65 37-72 29-6 55 15 53 44-8-12-20-18-33-18-8 15-26 24-52 25l-5 21Z" fill="#173B57"/>
              <path d="M210 132c5 3 10 3 15 0" stroke="#A65C4B" strokeWidth="3" strokeLinecap="round"/>
              <circle cx="208" cy="119" r="3" fill="#263C4C"/><circle cx="238" cy="119" r="3" fill="#263C4C"/>
              <path d="M160 235c-6-35 0-65 20-83l29 14-4 69h-45Z" fill="#13A77D"/>
              <path d="M214 236c-3-28 7-55 24-67l27 8 13 59h-64Z" fill="#1B5D70"/>
              <path d="M192 173c-13 20-16 35-12 52" stroke="#F4B88F" strokeWidth="12" strokeLinecap="round"/>
              <path d="M184 224 151 239" stroke="#F4B88F" strokeWidth="10" strokeLinecap="round"/>
              <rect x="142" y="232" width="52" height="7" rx="3.5" fill="#173B57"/>
              <path d="M266 232h-20l-9-82h29l12 82Z" fill="#D9E8F4" stroke="#9DB8CB" strokeWidth="3"/>
              <path d="M236 150h33l-4-12h-26l-3 12Z" fill="#13B981"/>
              <path d="M65 236h72" stroke="#173B57" strokeWidth="6" strokeLinecap="round"/>
              <path d="m92 86 9-16 9 16-9 16-9-16Z" fill="#F5C96B"/>
              <path d="M359 64v-17m-8 8h16" stroke="#E4AD45" strokeWidth="4" strokeLinecap="round"/>
              <circle cx="80" cy="60" r="6" fill="#17B987"/><circle cx="381" cy="205" r="5" fill="#F4C66D"/>
            </svg>
          </div>
          <div className="hero-floating-card"><span className="floating-check">✓</span><span><strong>Keep learning</strong><small>Progress starts today</small></span></div>
        </div>
      </section>

      <section className="daily-tip"><span className="tip-icon" aria-hidden="true">✦</span><p><strong>A little every day.</strong> Small, consistent practice is the key to lasting progress.</p></section>

      <section className="home-stats" aria-label="Your learning statistics" aria-busy={stats.status === "loading"}>
        <article className="home-stat-card"><span className="home-stat-icon score-icon" aria-hidden="true">↗</span><div><p>AVERAGE SCORE</p><strong>{average}</strong><small>Across completed attempts</small></div></article>
        <article className="home-stat-card"><span className="home-stat-icon completed-icon" aria-hidden="true">✓</span><div><p>COMPLETED TESTS</p><strong>{completed}</strong><small>Every session counts</small></div></article>
        {stats.status === "error" && <div className="stats-error"><div className="error-msg" role="alert">{STATS_FAILED}</div><button className="btn btn-secondary" onClick={() => setStatsReload((key) => key + 1)}>Try again</button></div>}
      </section>

      <section className="quick-access-section">
        <div className="home-section-heading"><div><p className="eyebrow">GET STARTED</p><h2>Quick access</h2></div><span>Choose how you want to learn</span></div>
        <div className="quick-access-grid">
          <button className="quick-card" type="button" onClick={() => navigate("/practice")}>
            <span className="quick-icon quick-dark" aria-hidden="true">◷</span><strong>Full Test</strong><p>Take a complete TOEIC test, all 7 Parts in one timed session.</p><small>120 MINUTES · TIMED</small>
          </button>
          <button className="quick-card" type="button" onClick={() => navigate("/practice")}>
            <span className="quick-icon quick-green" aria-hidden="true">♫</span><strong>Practice by Test</strong><p>Pick a Test, then take every Part or only the Parts you want.</p><small>CHOOSE YOUR PARTS</small>
          </button>
          <button className="quick-card" type="button" onClick={() => navigate("/progress")}>
            <span className="quick-icon quick-dark" aria-hidden="true">▥</span><strong>My Progress</strong><p>Review your completed attempts and track your scores.</p><small>YOUR LEARNING HISTORY</small>
          </button>
        </div>
      </section>

      <section className="jump-section" aria-labelledby="tests-heading">
        <div className="home-section-heading"><div><p className="eyebrow">PRACTICE BY TEST</p><h2 id="tests-heading">Jump into a Test</h2></div><Link to="/practice" className="muted-label">See all Tests</Link></div>
        {testStartError && <div className="error-msg" role="alert">{testStartError}</div>}
        {tests.status === "loading" && <div role="status" className="part-loading">Loading available Tests…</div>}
        {tests.status === "error" && <div><div className="error-msg" role="alert">{TESTS_FAILED}</div><button type="button" className="btn btn-secondary" onClick={() => setTestsReload((key) => key + 1)}>Try again</button></div>}
        {tests.status === "empty" && <div className="card empty-state"><p>No Tests are published yet.</p></div>}
        {tests.status === "ready" && <ul className="dash-test-list">{tests.items.map((test) => (
          <li className="dash-test" key={test.id}>
            <div><strong>{test.title}</strong><p>{(test.parts || []).length} Parts · {(test.parts || []).reduce((sum, p) => sum + (p.questionCount || 0), 0)} questions</p></div>
            <div className="dash-test-actions">
              <button type="button" className="btn btn-green" aria-label={`Start full ${test.title}`} disabled={startingTest !== null} onClick={() => startTest(test)}>{startingTest === test.id ? "Starting…" : "Start full test"}</button>
              <Link to="/practice" className="btn btn-secondary" aria-label={`Choose Parts of ${test.title}`}>Choose Parts</Link>
            </div>
          </li>
        ))}</ul>}
      </section>
    </>
  );
}
