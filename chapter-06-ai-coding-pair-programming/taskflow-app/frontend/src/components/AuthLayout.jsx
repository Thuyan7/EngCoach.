// Split layout from the approved Chapter 4 prototype (Concept C, Login
// screen): brand panel on the left, form card on the right. Register reuses
// it unchanged — the prototype has no Register screen, see SLICE-S5B-NOTES.md.
export default function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <main className="login-split">
      <div className="login-brandside">
        <div className="brand-inner">
          <div className="wordmark light">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
            </svg>
            EngCoach
          </div>
          <h2 className="brand-title">
            Practice TOEIC Reading and Listening with instant, explained feedback.
          </h2>
          <ul className="brand-points">
            <li>Practice by individual TOEIC Part</li>
            <li>Full-length timed mock tests</li>
            <li>An explanation for every wrong answer</li>
          </ul>
        </div>
      </div>
      <div className="login-formside">
        <div className="login-card">
          <h1 className="login-title">{title}</h1>
          <p className="login-sub">{subtitle}</p>
          {children}
          {footer}
        </div>
      </div>
    </main>
  );
}
