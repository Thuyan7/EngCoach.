import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

// Sidebar shell from the approved prototype (Dashboard / Practice / Progress).
// Practice remains disabled until a dedicated practice index is in scope.
export default function AppShell() {
  const { signOut } = useAuth();
  return (
    <div className="shell">
      <nav className="sidebar" aria-label="Main navigation">
        <div className="wordmark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
          </svg>
          EngCoach
        </div>

        <p className="nav-group-label">Practice</p>
        <ul className="navlist">
          <li>
            <NavLink to="/dashboard" className="nav-item">
              Dashboard
            </NavLink>
          </li>
          <li>
            <NavLink to="/tests" className="nav-item">
              Practice
            </NavLink>
          </li>
          <li>
            <NavLink to="/progress" className="nav-item">
              Progress
            </NavLink>
          </li>
        </ul>

        <div className="sidebar-foot">
          <button type="button" className="btn btn-secondary btn-block" onClick={signOut}>
            Log out
          </button>
        </div>
      </nav>

      <div className="content-col">
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
