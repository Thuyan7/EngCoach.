import { useAuth } from "../auth/AuthContext.jsx";

// PLACEHOLDER. S5b's definition of done only requires a redirect target
// after login. The real Dashboard (stats, Part list) is slice S6b; this page
// shows no mock data on purpose.
export default function DashboardPage() {
  const { signOut } = useAuth();
  return (
    <main className="placeholder">
      <h1 className="login-title">Dashboard</h1>
      <p className="login-sub">You are signed in. The real Dashboard is built in slice S6b.</p>
      <button type="button" className="btn btn-secondary" onClick={signOut}>
        Log out
      </button>
    </main>
  );
}
