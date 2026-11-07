import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

const navItems = [
  { to: "/dashboard", label: "Dashboard", icon: "M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z" },
  { to: "/practice", label: "Practice", icon: "M8 5v14l11-7z" },
  { to: "/progress", label: "Progress", icon: "M4 19V5m0 14h17M7 15l4-4 3 2 6-7" },
];

export default function AppShell() {
  const { signOut } = useAuth();
  return (
    <div className="shell">
      <aside className="sidebar">
        <NavLink to="/dashboard" className="wordmark" aria-label="EngCoach home">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
              <path d="M8 7h8M8 11h6" />
            </svg>
          </span>
          <span>Eng<span className="wordmark-accent">Coach</span></span>
        </NavLink>

        <div className="sidebar-section">
          <p className="nav-group-label">LEARNING SPACE</p>
          <nav aria-label="Main navigation">
            <ul className="navlist">
              {navItems.map((item) => (
                <li key={item.to}>
                  <NavLink to={item.to} end className={({ isActive }) => `nav-item${isActive ? " active" : ""}`}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <path d={item.icon} />
                    </svg>
                    <span>{item.label}</span>
                    {item.to === "/practice" && <span className="nav-item-arrow" aria-hidden="true">›</span>}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </div>

        <div className="sidebar-tip">
          <span className="sidebar-tip-icon" aria-hidden="true">✦</span>
          <strong>Small steps, big progress.</strong>
          <p>A little practice every day makes a difference.</p>
        </div>

        <div className="sidebar-foot">
          <button type="button" className="logout-button" onClick={signOut}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M10 17l5-5-5-5M15 12H3" /><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6" />
            </svg>
            Log out
          </button>
        </div>
      </aside>

      <div className="content-col">
        <header className="topbar">
          <div className="topbar-context"><span className="topbar-dot" /> TOEIC LEARNING PLATFORM</div>
          <span className="topbar-note">Learn at your own pace</span>
        </header>
        <main className="main">
          <Outlet />
        </main>
        <footer className="app-footer">EngCoach <span>·</span> Build confidence, one question at a time.</footer>
      </div>
    </div>
  );
}
