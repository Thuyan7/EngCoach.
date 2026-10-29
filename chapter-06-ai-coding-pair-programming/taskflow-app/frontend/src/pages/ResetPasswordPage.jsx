import { useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { resetPassword } from "../api/auth.js";
import { useAuth } from "../auth/AuthContext.jsx";
import AuthLayout from "../components/AuthLayout.jsx";
import FormField from "../components/FormField.jsx";

export default function ResetPasswordPage() {
  const { isAuthenticated } = useAuth();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [complete, setComplete] = useState(false);

  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    if (!token) {
      setError("This reset link is missing its token. Request a new password-reset link.");
      return;
    }
    if (password.length < 8) {
      setError("Your new password must be at least 8 characters.");
      return;
    }
    if (password !== confirmPassword) {
      setError("The passwords do not match.");
      return;
    }
    setSubmitting(true);
    try {
      await resetPassword(token, password);
      setComplete(true);
    } catch (err) {
      if (err.status === 410) {
        setError("This reset link has expired or has already been used. Request a new one.");
      } else if (err.status === 422) {
        setError(err.message || "Please check your new password and try again.");
      } else {
        setError(err.status === 0 || err.status >= 500
          ? "We couldn't reach the server. Please try again."
          : "This reset link could not be used. Request a new one.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Choose a new password"
      subtitle="Use at least 8 characters for your new password."
      footer={<p className="login-foot"><Link to="/login">Back to log in</Link></p>}
    >
      {complete ? (
        <div className="success-msg" role="status" aria-live="polite">
          Your password has been updated. You can now <Link to="/login">log in</Link> with your new password.
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate aria-busy={submitting}>
          {error && <div className="error-msg" role="alert">{error}</div>}
          <FormField
            id="reset-password"
            label="New password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <FormField
            id="reset-confirm-password"
            label="Confirm new password"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
          />
          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? "Updating…" : "Update password"}
          </button>
        </form>
      )}
    </AuthLayout>
  );
}
