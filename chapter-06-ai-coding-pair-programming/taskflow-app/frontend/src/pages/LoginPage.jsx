import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { login } from "../api/auth.js";
import { useAuth } from "../auth/AuthContext.jsx";
import AuthLayout from "../components/AuthLayout.jsx";
import FormField from "../components/FormField.jsx";

// Copy taken from the approved prototype's Login screen.
const WRONG_CREDENTIALS = "Email or password is incorrect.";
const UNREACHABLE = "We couldn't reach the server. Please try again.";

export default function LoginPage() {
  const { isAuthenticated, signIn } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const authResponse = await login(email, password);
      signIn(authResponse);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      // feature-spec §2 / AC-002.2: never disclose which field was wrong, so
      // every 4xx collapses into the same message. Only "no response" and
      // 5xx are told apart, because they are not a credentials problem.
      setError(err.status === 0 || err.status >= 500 ? UNREACHABLE : WRONG_CREDENTIALS);
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Log in"
      subtitle="Continue your TOEIC practice."
      footer={
        <>
          <p className="login-foot">Forgot your password?</p>
          <p className="login-foot">
            New to EngCoach? <Link to="/register">Create an account</Link>
          </p>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
        {error && (
          <div className="error-msg" role="alert">
            {error}
          </div>
        )}
        <FormField
          id="login-email"
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <FormField
          id="login-password"
          label="Password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? "Logging in…" : "Log in"}
        </button>
      </form>
    </AuthLayout>
  );
}
