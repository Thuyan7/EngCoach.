import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

const navItems = [
  { to: "/dashboard", label: "Home", icon: "M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z" },
  { to: "/practice", label: "Practice Tests", icon: "M8 5v14l11-7z" },
  { to: "/progress", label: "My Progress", icon: "M4 19V5m0 14h17M7 15l4-4 3 2 6-7" },
];

export default function AppShell() {
  const { signOut } = useAuth();
  return (
    <div className="shell">
      <header className="site-header">
        <div className="site-header-inner">
          <NavLink to="/dashboard" className="site-brand" aria-label="EngCoach home">
            <span className="site-brand-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 8.5 12 3l9 5.5v7L12 21l-9-5.5v-7Z" /><path d="M8 10h8M8 14h5" />
              </svg>
            </span>
            <span>Eng<span>Coach</span></span>
          </NavLink>
          <nav className="top-navigation" aria-label="Main navigation">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} end className={({ isActive }) => `top-nav-link${isActive ? " active" : ""}`}>
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="header-actions">
            <span className="header-tagline">Your TOEIC study space</span>
            <button type="button" className="header-signout" onClick={signOut}>Log out</button>
          </div>
        </div>
      </header>
      <main className="main"><Outlet /></main>
      <footer className="app-footer">EngCoach <span>·</span> Build confidence, one question at a time.</footer>
    </div>
  );
}
