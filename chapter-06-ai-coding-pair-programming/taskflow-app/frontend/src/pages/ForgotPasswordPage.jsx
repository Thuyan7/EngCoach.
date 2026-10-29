import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { forgotPassword } from "../api/auth.js";
import { useAuth } from "../auth/AuthContext.jsx";
import AuthLayout from "../components/AuthLayout.jsx";
import FormField from "../components/FormField.jsx";

export default function ForgotPasswordPage() {
  const { isAuthenticated } = useAuth();
  const [email, setEmail] = useState(""); const [error, setError] = useState(""); const [submitting, setSubmitting] = useState(false); const [sent, setSent] = useState(false);
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;
  async function handleSubmit(event) {
    event.preventDefault(); setError("");
    if (!email.trim()) return setError("Enter the email address associated with your account.");
    setSubmitting(true);
    try { await forgotPassword(email.trim()); setSent(true); }
    catch (err) { setError(err.status === 0 || err.status >= 500 ? "We couldn't reach the server. Please try again." : "We couldn't process that request. Please try again."); }
    finally { setSubmitting(false); }
  }
  return <AuthLayout title="Reset your password" subtitle="Enter your account email and we'll help you get back in." footer={<p className="login-foot"><Link to="/login">Back to log in</Link></p>}>
    {sent ? <div className="success-msg" role="status" aria-live="polite">If an account matches that email, password-reset instructions will be provided. Check your inbox and spam folder.</div> : <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
      {error && <div className="error-msg" role="alert">{error}</div>}
      <FormField id="forgot-email" label="Email" type="email" autoComplete="email" placeholder="you@example.com" value={email} onChange={e => setEmail(e.target.value)} />
      <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>{submitting ? "Sending…" : "Send reset instructions"}</button>
    </form>}
  </AuthLayout>;
}
